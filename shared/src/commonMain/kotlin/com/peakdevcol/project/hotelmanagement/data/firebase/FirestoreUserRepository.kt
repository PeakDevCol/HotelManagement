package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.UserDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toNewUserDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toUser
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

internal class FirestoreUserRepository(
    private val firestore: Lazy<FirebaseFirestore>,
) : UserRepository {

    override fun observeUser(userId: String): Flow<User?> =
        firestore.value
            .collection(FirestorePaths.USERS)
            .document(userId)
            .snapshots
            .map { snapshot -> if (snapshot.exists) snapshot.data<UserDto>().toUser(userId) else null }

    override fun observeEmployees(): Flow<List<User>> =
        firestore.value
            .collection(FirestorePaths.USERS)
            .snapshots
            .map { employees -> employees.documents.mapNotNull { it.data<UserDto>().toUser(it.id) } }

    override suspend fun createProfile(user: User, createdBy: String): Result<Unit> = write {
        users.document(user.id).set(user.toNewUserDto(createdBy))
    }

    // Only what an administrator may change is written: the email, who created the employee and when stay as they are.
    override suspend fun updateProfile(user: User): Result<Unit> = write {
        users.document(user.id).update(
            "fullName" to user.fullName,
            "type" to user.type.code(),
            "area" to user.area.code(),
            "active" to user.active,
        )
    }

    private val users get() = firestore.value.collection(FirestorePaths.USERS)

    // Managing employees is done with connection: a change that only stays on the device could
    // leave a deactivated employee with access, so no answer from the server is a failure (RF-EMP-02).
    private suspend fun write(change: suspend () -> Unit): Result<Unit> = try {
        withTimeoutOrNull(SERVER_WAIT) { change() }?.let { Result.success(Unit) } ?: Result.failure(EmployeeError.Network)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val denied = (e as? FirebaseFirestoreException)?.code == FirestoreExceptionCode.PERMISSION_DENIED
        Result.failure(if (denied) EmployeeError.NotAllowed else EmployeeError.Unknown)
    }

    private companion object {
        val SERVER_WAIT = 10.seconds
    }
}
