package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import kotlinx.coroutines.flow.Flow

/** RF-HAB-01: the types of room at `RoomTypes`. */
interface RoomTypeRepository {
    /** Every type, active or not, and its changes, in no particular order. */
    fun observeRoomTypes(): Flow<List<RoomType>>

    /** Creates the type, or replaces the one with the same id. Types are deactivated, never deleted. */
    suspend fun save(type: RoomType): Result<Unit>
}

/** RF-HAB-02: the rooms at `Rooms`. The hotel has a few dozen, so all of them are listened to. */
interface RoomRepository {
    /** Every room, active or not, and its changes, in no particular order. */
    fun observeRooms(): Flow<List<Room>>

    /** Creates the room with its state as given: free and clean (decision 4A). */
    suspend fun create(room: Room): Result<Unit>

    /** Changes the number, the type, the floor and whether it is active, never its state. */
    suspend fun updateData(room: Room): Result<Unit>
}

/** RF-HAB-03: the housekeeping marked by hand, until the cleaning tasks of phase 4 replace it. */
interface HousekeepingRepository {
    /** Moves the room to [housekeeping], signed by [markedBy] with the time of the server. */
    suspend fun mark(roomId: String, housekeeping: Housekeeping, markedBy: String): Result<Unit>
}
