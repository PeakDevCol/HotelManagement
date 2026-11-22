package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal const val ROOM_NUMBER_FIELD = "number"
internal const val ROOM_TYPE_ID_FIELD = "roomTypeId"
internal const val ROOM_FLOOR_FIELD = "floor"
internal const val ROOM_ACTIVE_FIELD = "active"
internal const val HOUSEKEEPING_FIELD = "housekeeping"
internal const val HOUSEKEEPING_CHANGED_BY_FIELD = "housekeepingChangedBy"
internal const val HOUSEKEEPING_CHANGED_AT_FIELD = "housekeepingChangedAt"

/**
 * Runs a write to the rooms. Firestore applies it on the device at once and sends it when there is
 * connection (RNF-04). The wait is only there to hear a rejection from the server, like the one a
 * non-administrator gets from the rules, which then reads as [deniedAs].
 */
internal suspend fun roomWrite(
    serverWait: Duration,
    deniedAs: RoomError = RoomError.NotAllowed,
    write: suspend () -> Unit,
): Result<Unit> = try {
    withTimeoutOrNull(serverWait) { write() }
    Result.success(Unit)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    val denied = (e as? FirebaseFirestoreException)?.code == FirestoreExceptionCode.PERMISSION_DENIED
    Result.failure(if (denied) deniedAs else RoomError.Unknown)
}
