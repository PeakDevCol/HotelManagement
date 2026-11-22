package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.repository.HousekeepingRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlin.time.Duration

internal class FirestoreHousekeepingRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
) : HousekeepingRepository {

    private val rooms get() = firestore.value.collection(FirestorePaths.ROOMS)

    // The rules reject a room that is no longer as the employee saw it: occupied, or already marked
    // by someone else. An employee with the app open is active, so that is the reason they give.
    override suspend fun mark(roomId: String, housekeeping: Housekeeping, markedBy: String): Result<Unit> =
        roomWrite(serverWait, deniedAs = RoomError.Changed) {
            rooms.document(roomId).updateFields {
                HOUSEKEEPING_FIELD to housekeeping.code()
                HOUSEKEEPING_CHANGED_BY_FIELD to markedBy
                HOUSEKEEPING_CHANGED_AT_FIELD to FieldValue.serverTimestamp
            }
        }
}
