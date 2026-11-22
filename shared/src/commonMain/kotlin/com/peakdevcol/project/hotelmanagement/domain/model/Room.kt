package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/** RF-HAB-01: a kind of room. A reservation copies its rate for each of its nights (M11). */
data class RoomType(
    val id: String,
    val name: String,
    /** How many people sleep in it, children included. */
    val capacity: Int,
    /** Decision 3A: what the guest pays for one night, in pesos, with the tax included, like the price of a product. */
    val baseRate: Long,
    val taxId: String? = null,
    /** An inactive type stays for the rooms that have it, but a new room cannot take it. */
    val active: Boolean = true,
)

const val MAX_ROOM_CAPACITY = 20
const val MAX_ROOM_NUMBER = 9_999
const val MAX_ROOM_FLOOR = 99

/** Whether a guest is in the room. Only the check-in and the check-out change it (RF-EST-01, RF-EST-07). */
enum class Occupancy {
    VACANT,
    OCCUPIED,
}

/** How clean the room is (section 6.8). From phase 4 the cleaning tasks move it. */
enum class Housekeeping {
    DIRTY,
    CLEANING,
    CLEAN,
}

/** RF-HAB-02 and RF-HAB-03: a room of the hotel, with its state in independent parts (section 6.8). */
data class Room(
    val id: String,
    /** Unique in the hotel. Orders to a room use the same number (RF-ORD-03). */
    val number: Int,
    val roomTypeId: String,
    val floor: Int,
    val occupancy: Occupancy = Occupancy.VACANT,
    /** Decision 4A: a new room starts free and clean. */
    val housekeeping: Housekeeping = Housekeeping.CLEAN,
    /** Only while occupied: the check-in sets it and the check-out clears it (M12). */
    val expectedCheckOutDate: CalendarDate? = null,
    /** A room is deactivated, never deleted: its reservations keep pointing to it. */
    val active: Boolean = true,
    /** Decision 5A: who marked the housekeeping by hand the last time, and when. */
    val housekeepingChangedBy: String? = null,
    val housekeepingChangedAt: Instant? = null,
)

/**
 * RF-HAB-04: the one state the board shows for a room (section 6.8, P31), in the order of its life:
 * free, occupied, to be cleaned and being cleaned. "En mantenimiento" comes with the blocks of the
 * rooms (RF-MAN-03, M14).
 */
enum class RoomStatus {
    AVAILABLE,
    OCCUPIED,
    DIRTY,
    CLEANING,
}

val Room.status: RoomStatus
    get() = when {
        occupancy == Occupancy.OCCUPIED -> RoomStatus.OCCUPIED
        housekeeping == Housekeeping.CLEANING -> RoomStatus.CLEANING
        housekeeping == Housekeeping.DIRTY -> RoomStatus.DIRTY
        else -> RoomStatus.AVAILABLE
    }

/**
 * RF-HAB-03 with decision 5A: until the cleaning tasks of phase 4, any employee marks a free room
 * clean when it is to be cleaned, or back to be cleaned to undo a mistake. Null when it cannot be
 * marked by hand: while it is occupied or inactive, or in the middle of a cleaning.
 */
val Room.markableAs: Housekeeping?
    get() = when {
        !active || occupancy == Occupancy.OCCUPIED -> null
        housekeeping == Housekeeping.DIRTY -> Housekeeping.CLEAN
        housekeeping == Housekeeping.CLEAN -> Housekeeping.DIRTY
        else -> null
    }

/** Why a change to the rooms or their types was not saved. */
sealed class RoomError(message: String) : Exception(message) {
    data object BlankName : RoomError("The name is required")
    data object DuplicateTypeName : RoomError("There is already a type with that name")
    data object InvalidCapacity : RoomError("The capacity must be from 1 to $MAX_ROOM_CAPACITY people")
    data object InvalidRate : RoomError("The rate must be greater than zero")
    data object InvalidNumber : RoomError("The number must be from 1 to $MAX_ROOM_NUMBER")
    data object DuplicateNumber : RoomError("There is already a room with that number")
    data object InvalidFloor : RoomError("The floor must be from 0 to $MAX_ROOM_FLOOR")
    data object MissingType : RoomError("The room needs an active type")

    /** The room is no longer as the employee saw it, for example because someone else marked it. */
    data object Changed : RoomError("The room changed in the meantime")
    data object NotFound : RoomError("It no longer exists")
    data object NoSession : RoomError("There is no session")
    data object NotAllowed : RoomError("Only an administrator changes the rooms")
    data object Unknown : RoomError("Unknown error")
}
