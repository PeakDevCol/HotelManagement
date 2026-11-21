package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.AttendanceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.UserDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toUser
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.ReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReceptionSession
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Transaction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.memoryCacheSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * RF-ASI-11 and V1: the employee signs in on a second Firebase instance, which has its own session,
 * so the session of the computer stays as it is. Marking here needs connection, and each mark is a
 * transaction: a plain write would wait in the queue of that employee and go out the next time they
 * sign in here, after they were told it failed.
 */
internal class FirebaseReceptionMarkingRepository(
    private val receptionApp: Lazy<FirebaseApp>,
    private val serverWait: Duration = 15.seconds,
) : ReceptionMarkingRepository {

    private val auth get() = Firebase.auth(receptionApp.value)

    // The computer is shared: what each employee reads is kept in memory only, not on its disk.
    private val firestore by lazy {
        Firebase.firestore(receptionApp.value).apply { settings = firestoreSettings { cacheSettings = memoryCacheSettings { } } }
    }

    override suspend fun <T> asEmployee(email: String, password: String, action: suspend (ReceptionSession) -> Result<T>): Result<T> =
        try {
            val account = withTimeoutOrNull(serverWait) { auth.signInWithEmailAndPassword(email, password).user }
                ?: return Result.failure(AttendanceError.Network)
            val employee = withTimeoutOrNull(serverWait) { profileOf(account.uid) }
                ?: return Result.failure(AttendanceError.Network)
            if (!employee.active) return Result.failure(AttendanceError.InactiveAccount)
            action(Session(employee, firestore, serverWait))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toSignInError())
        } finally {
            runCatching { auth.signOut() }
        }

    private suspend fun profileOf(accountId: String): User {
        val snapshot = firestore.collection(FirestorePaths.USERS).document(accountId).get(Source.SERVER)
        if (!snapshot.exists) throw AttendanceError.InactiveAccount
        return snapshot.data<UserDto>().toUser(accountId) ?: throw AttendanceError.InactiveAccount
    }

    private class Session(
        override val employee: User,
        private val firestore: FirebaseFirestore,
        private val serverWait: Duration,
    ) : ReceptionSession {
        private val attendance get() = firestore.collection(FirestorePaths.ATTENDANCE)

        override suspend fun openEntry(): Result<AttendanceRecord?> = try {
            withTimeoutOrNull(serverWait) { attendance.openEntriesOf(employee.id).get(Source.SERVER) }
                ?.let { Result.success(it.toOpenEntry()) }
                ?: Result.failure(AttendanceError.Network)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAttendanceError())
        }

        override suspend fun saveEntry(record: AttendanceRecord): Result<MarkDelivery> = inTransaction {
            set(attendance.document(record.id), record.toDto())
            Result.success(MarkDelivery.SENT)
        }

        // The entry is read again inside the transaction: someone else may have closed it meanwhile.
        override suspend fun saveExit(recordId: String, exit: Mark): Result<MarkDelivery> = inTransaction {
            val reference = attendance.document(recordId)
            val stillOpen = get(reference).let { it.exists && it.data<AttendanceDto>(ServerTimestampBehavior.NONE).clockOutDeviceTime == null }
            if (!stillOpen) return@inTransaction Result.failure(AttendanceError.NoOpenEntry)
            updateFields(reference) { exitFields(exit) }
            Result.success(MarkDelivery.SENT)
        }

        private suspend fun inTransaction(change: suspend Transaction.() -> Result<MarkDelivery>): Result<MarkDelivery> = try {
            withTimeoutOrNull(serverWait) { firestore.runTransaction(change) } ?: Result.failure(AttendanceError.Network)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAttendanceError())
        }
    }

    private fun Exception.toSignInError(): AttendanceError = when {
        this is AttendanceError -> this
        this is FirebaseAuthInvalidCredentialsException || this is FirebaseAuthInvalidUserException -> AttendanceError.InvalidCredentials
        // The desktop SDK only passes on the error code of the Firebase Auth server in the message.
        CREDENTIAL_CODES.any { message?.contains(it) == true } -> AttendanceError.InvalidCredentials
        this is FirebaseNetworkException -> AttendanceError.Network
        else -> toAttendanceError()
    }

    private companion object {
        val CREDENTIAL_CODES = listOf("INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND", "INVALID_EMAIL", "USER_DISABLED")
    }
}
