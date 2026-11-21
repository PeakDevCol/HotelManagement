package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.AttendanceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRegistrationDto
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceCorrectionRepository
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Transaction
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * RF-ASI-07: each change of the administrator is a transaction, so it needs connection (RNF-04).
 * A plain write would wait on the device and could reach the server after the employee marked,
 * over a record that is no longer the one the administrator saw.
 */
internal class FirestoreAttendanceCorrectionRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = 15.seconds,
) : AttendanceCorrectionRepository {

    private val attendance get() = firestore.value.collection(FirestorePaths.ATTENDANCE)

    // The record is read again inside the transaction: the employee may have marked the exit meanwhile.
    override suspend fun addCorrection(recordId: String, correction: Correction, missingExit: Mark?): Result<Unit> = inTransaction {
        val reference = attendance.document(recordId)
        val snapshot = get(reference)
        if (!snapshot.exists) return@inTransaction Result.failure(CorrectionError.Changed)
        val hasExit = snapshot.data<AttendanceDto>(ServerTimestampBehavior.NONE).clockOutDeviceTime != null
        val isStillAsSeen = if (missingExit != null) !hasExit else correction.mark != MarkKind.EXIT || hasExit
        if (!isStillAsSeen) return@inTransaction Result.failure(CorrectionError.Changed)
        updateFields(reference) {
            FieldPath(CORRECTIONS_FIELD, correction.id) to correction.toDto()
            LAST_CORRECTION_ID_FIELD to correction.id
            if (missingExit != null) adminExitFields(missingExit)
        }
        Result.success(Unit)
    }

    override suspend fun register(record: AttendanceRecord): Result<Unit> = inTransaction {
        set(attendance.document(record.id), record.toRegistrationDto())
        Result.success(Unit)
    }

    private suspend fun inTransaction(change: suspend Transaction.() -> Result<Unit>): Result<Unit> = try {
        withTimeoutOrNull(serverWait) { firestore.value.runTransaction(change) } ?: Result.failure(CorrectionError.Network)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toCorrectionError())
    }

    private fun Exception.toCorrectionError(): CorrectionError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.PERMISSION_DENIED -> CorrectionError.NotAllowed
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> CorrectionError.Network
        else -> CorrectionError.Unknown
    }
}
