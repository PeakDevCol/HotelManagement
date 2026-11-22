package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.RoomTypeDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRoomType
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class FirestoreRoomTypeRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
) : RoomTypeRepository {

    private val types get() = firestore.value.collection(FirestorePaths.ROOM_TYPES)

    // A handful of types: the whole collection is listened to.
    override fun observeRoomTypes(): Flow<List<RoomType>> =
        types.snapshots.map { snapshot -> snapshot.documents.map { it.data<RoomTypeDto>().toRoomType(it.id) } }

    override suspend fun save(type: RoomType): Result<Unit> = roomWrite(serverWait) {
        types.document(type.id).set(type.toDto())
    }
}
