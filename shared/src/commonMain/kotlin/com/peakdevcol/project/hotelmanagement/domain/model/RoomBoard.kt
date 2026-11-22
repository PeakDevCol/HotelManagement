package com.peakdevcol.project.hotelmanagement.domain.model

/** A room as the board shows it: with its type and the name of who marked its housekeeping by hand. */
data class BoardRoom(
    val room: Room,
    /** Null when the type no longer exists. The room is still shown, so nothing gets lost from sight. */
    val type: RoomType?,
    val housekeepingChangedByName: String? = null,
) {
    val status: RoomStatus get() = room.status
}

/** The rooms of one floor, by number. */
data class RoomFloor(val floor: Int, val rooms: List<BoardRoom>)

/**
 * RF-HAB-04: every active room, live. An administrator also gets the inactive rooms, to turn them on
 * again, since types and rooms are set up from the board itself (decision 1A).
 */
data class RoomBoard(
    val rooms: List<BoardRoom> = emptyList(),
    val inactiveRooms: List<BoardRoom> = emptyList(),
    /** Every type, active or not, by name. */
    val types: List<RoomType> = emptyList(),
    val canManage: Boolean = false,
) {
    fun roomOf(roomId: String): BoardRoom? = (rooms + inactiveRooms).firstOrNull { it.room.id == roomId }
}

/** The floors from the lowest, each with its rooms by number. */
fun List<BoardRoom>.byFloor(): List<RoomFloor> =
    groupBy { it.room.floor }
        .entries
        .sortedBy { it.key }
        .map { (floor, rooms) -> RoomFloor(floor, rooms.sortedBy { it.room.number }) }

/** RF-HAB-01: the types of room and the taxes their rate can include (P27). */
data class RoomTypeCatalog(
    /** By name, active or not. */
    val types: List<RoomType> = emptyList(),
    val taxes: List<Tax> = emptyList(),
) {
    fun taxOf(type: RoomType): Tax? = taxes.firstOrNull { it.id == type.taxId }
}
