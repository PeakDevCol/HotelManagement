package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.AttendanceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRecord
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toTimestamp
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FieldsAndValuesUpdateDSL
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal const val EMPLOYEE_ID_FIELD = "employeeId"
internal const val CLOCK_IN_AT_FIELD = "clockInAt"
internal const val CLOCK_OUT_AT_FIELD = "clockOutAt"
private const val CLOCK_OUT_DEVICE_TIME_FIELD = "clockOutDeviceTime"
private const val CLOCK_OUT_SOURCE_FIELD = "clockOutSource"
internal const val CORRECTIONS_FIELD = "corrections"
internal const val LAST_CORRECTION_ID_FIELD = "lastCorrectionId"

/**
 * The entry without exit of an employee. Two equalities need no composite index, and the query
 * asks for the employee's own marks, the only ones the rules let them read.
 */
internal fun CollectionReference.openEntriesOf(employeeId: String): Query =
    where { all(EMPLOYEE_ID_FIELD equalTo employeeId, CLOCK_OUT_AT_FIELD equalTo null) }

/** The time of the server reads as null while the mark has not reached it. */
internal fun QuerySnapshot.toRecords(): List<AttendanceRecord> =
    documents.mapNotNull { it.data<AttendanceDto>(ServerTimestampBehavior.NONE).toRecord(it.id) }

internal fun QuerySnapshot.toOpenEntry(): AttendanceRecord? = toRecords().maxByOrNull { it.clockIn.deviceTime }

/** The exit closes the entry: only these fields change, as the rules require. */
internal fun FieldsAndValuesUpdateDSL.exitFields(exit: Mark) {
    CLOCK_OUT_AT_FIELD to FieldValue.serverTimestamp
    CLOCK_OUT_DEVICE_TIME_FIELD to exit.deviceTime.toTimestamp()
    CLOCK_OUT_SOURCE_FIELD to exit.source.code()
    "clockOutDeviceId" to exit.deviceId
    "clockOutDistanceMeters" to exit.distanceMeters
    "clockOutAccuracyMeters" to exit.accuracyMeters
}

/**
 * RF-ASI-06: the exit the administrator writes for an entry without one, with the time they gave
 * as both times. The rest of the exit stays empty, as the rules require.
 */
internal fun FieldsAndValuesUpdateDSL.adminExitFields(exit: Mark) {
    CLOCK_OUT_AT_FIELD to exit.deviceTime.toTimestamp()
    CLOCK_OUT_DEVICE_TIME_FIELD to exit.deviceTime.toTimestamp()
    CLOCK_OUT_SOURCE_FIELD to exit.source.code()
}

/**
 * Firestore keeps the mark on the device at once and sends it when there is connection
 * (RF-ASI-05). Without an answer from the server in [serverWait] the mark is pending.
 */
internal suspend fun deliverMark(serverWait: Duration, write: suspend () -> Unit): Result<MarkDelivery> = try {
    val confirmed = withTimeoutOrNull(serverWait) { write() }
    Result.success(if (confirmed == null) MarkDelivery.PENDING else MarkDelivery.SENT)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toAttendanceError())
}

internal fun Exception.toAttendanceError(): AttendanceError = when ((this as? FirebaseFirestoreException)?.code) {
    FirestoreExceptionCode.PERMISSION_DENIED -> AttendanceError.NotAllowed
    FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> AttendanceError.Network
    else -> AttendanceError.Unknown
}
