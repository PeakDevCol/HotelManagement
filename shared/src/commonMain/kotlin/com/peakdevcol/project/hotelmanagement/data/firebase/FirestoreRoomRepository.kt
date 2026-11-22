package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.RoomDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class FirestoreRoomRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
) : RoomRepository {

    private val rooms get() = firestore.value.collection(FirestorePaths.ROOMS)

    // RNF-12: a few dozen rooms, each one a read when it changes. The time of a mark that has not
    // reached the server yet is estimated, so the board says when it was marked right away.
    override fun observeRooms(): Flow<List<Room>> =
        rooms.snapshots.map { snapshot -> snapshot.documents.map { it.data<RoomDto>(ServerTimestampBehavior.ESTIMATE).toRoom(it.id) } }

    override suspend fun create(room: Room): Result<Unit> = roomWrite(serverWait) {
        rooms.document(room.id).set(room.toDto())
    }

    // Only the data of the room, as the rules require: writing the whole document could undo a
    // cleaning marked meanwhile on another device.
    override suspend fun updateData(room: Room): Result<Unit> = roomWrite(serverWait) {
        rooms.document(room.id).updateFields {
            ROOM_NUMBER_FIELD to room.number
            ROOM_TYPE_ID_FIELD to room.roomTypeId
            ROOM_FLOOR_FIELD to room.floor
            ROOM_ACTIVE_FIELD to room.active
        }
    }
}
