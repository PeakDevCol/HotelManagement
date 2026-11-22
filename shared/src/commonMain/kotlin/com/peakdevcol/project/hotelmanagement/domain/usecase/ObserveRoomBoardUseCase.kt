package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.RoomBoard
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * RF-HAB-04: the rooms with their state, live, for every employee. Only an administrator sets them
 * up (RF-HAB-02), so only they get the inactive rooms (decision 1A).
 */
class ObserveRoomBoardUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val roomTypeRepository: RoomTypeRepository,
) {
    operator fun invoke(): Flow<RoomBoard> = combine(
        roomRepository.observeRooms(),
        roomTypeRepository.observeRoomTypes(),
        userRepository.observeEmployees(),
    ) { rooms, types, employees ->
        val userId = authRepository.currentUserId()
        val canManage = employees.any { it.id == userId && it.type == UserType.ADMIN }
        val typesById = types.associateBy { it.id }
        val names = employees.associate { it.id to it.fullName }
        val boardRooms = rooms
            .sortedWith(compareBy({ it.floor }, { it.number }))
            .map { room -> BoardRoom(room, typesById[room.roomTypeId], room.housekeepingChangedBy?.let(names::get)) }
        RoomBoard(
            rooms = boardRooms.filter { it.room.active },
            inactiveRooms = if (canManage) boardRooms.filterNot { it.room.active } else emptyList(),
            types = types.sortedBy { it.name.lowercase() },
            canManage = canManage,
        )
    }
}
