package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.MAX_ROOM_FLOOR
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_ROOM_NUMBER
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import kotlinx.coroutines.flow.first

/**
 * RF-HAB-02: creates a room, free and clean (decision 4A), or changes its number, type, floor and
 * whether it is active. Its state is never changed from here. Numbers are not repeated, active or
 * not, because orders and reservations find a room by its number.
 */
class SaveRoomUseCase(
    private val roomRepository: RoomRepository,
    private val roomTypeRepository: RoomTypeRepository,
) {
    /** A null [roomId] creates the room. */
    suspend operator fun invoke(roomId: String?, number: Int?, floor: Int?, roomTypeId: String?, active: Boolean = true): Result<Unit> {
        if (number == null || number !in 1..MAX_ROOM_NUMBER) return Result.failure(RoomError.InvalidNumber)
        if (floor == null || floor !in 0..MAX_ROOM_FLOOR) return Result.failure(RoomError.InvalidFloor)
        val rooms = roomRepository.observeRooms().first()
        if (rooms.any { it.id != roomId && it.number == number }) return Result.failure(RoomError.DuplicateNumber)
        val existing = roomId?.let { id -> rooms.firstOrNull { it.id == id } ?: return Result.failure(RoomError.NotFound) }
        // A room takes an active type; one that already has an inactive type may keep it.
        val type = roomTypeRepository.observeRoomTypes().first()
            .firstOrNull { it.id == roomTypeId && (it.active || it.id == existing?.roomTypeId) }
            ?: return Result.failure(RoomError.MissingType)
        return if (existing == null) {
            roomRepository.create(Room(id = newId(), number = number, roomTypeId = type.id, floor = floor, active = active))
        } else {
            roomRepository.updateData(existing.copy(number = number, roomTypeId = type.id, floor = floor, active = active))
        }
    }
}
