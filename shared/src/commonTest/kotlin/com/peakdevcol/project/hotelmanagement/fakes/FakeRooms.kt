package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.repository.HousekeepingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Instant

fun roomType(id: String, name: String, capacity: Int = 2, baseRate: Long = 250_000, taxId: String? = null, active: Boolean = true) =
    RoomType(id = id, name = name, capacity = capacity, baseRate = baseRate, taxId = taxId, active = active)

fun room(id: String, number: Int, roomTypeId: String = "double", floor: Int = number / 100) =
    Room(id = id, number = number, roomTypeId = roomTypeId, floor = floor)

class FakeRoomTypeRepository(vararg types: RoomType) : RoomTypeRepository {
    val types = MutableStateFlow(types.toList())
    var saveFailure: RoomError? = null

    override fun observeRoomTypes(): Flow<List<RoomType>> = types

    override suspend fun save(type: RoomType): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        types.value = types.value.filterNot { it.id == type.id } + type
        return Result.success(Unit)
    }
}

/**
 * The rooms of the hotel, and the housekeeping marked by hand on them: like Firestore, a mark
 * changes the same room the board listens to, signed with the time of [now].
 */
class FakeRoomRepository(vararg rooms: Room, private val now: Instant = Instant.parse("2026-11-21T14:00:00Z")) :
    RoomRepository, HousekeepingRepository {
    val rooms = MutableStateFlow(rooms.toList())
    var readFailure: Throwable? = null
    var saveFailure: RoomError? = null
    var markFailure: RoomError? = null
    val created = mutableListOf<Room>()
    val updated = mutableListOf<Room>()

    override fun observeRooms(): Flow<List<Room>> = readFailure?.let { error -> flow { throw error } } ?: rooms

    override suspend fun create(room: Room): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        created += room
        rooms.value += room
        return Result.success(Unit)
    }

    // Like the update of Firestore, only the data of the room changes, never its state.
    override suspend fun updateData(room: Room): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        updated += room
        rooms.value = rooms.value.map { stored ->
            if (stored.id == room.id) stored.copy(number = room.number, roomTypeId = room.roomTypeId, floor = room.floor, active = room.active) else stored
        }
        return Result.success(Unit)
    }

    override suspend fun mark(roomId: String, housekeeping: Housekeeping, markedBy: String): Result<Unit> {
        markFailure?.let { return Result.failure(it) }
        rooms.value = rooms.value.map { stored ->
            if (stored.id == roomId) stored.copy(housekeeping = housekeeping, housekeepingChangedBy = markedBy, housekeepingChangedAt = now) else stored
        }
        return Result.success(Unit)
    }
}
