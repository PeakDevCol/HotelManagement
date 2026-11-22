package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** A type of room stored at `RoomTypes/{id}` (RF-HAB-01). The rate is in pesos and includes the tax. */
@Serializable
internal data class RoomTypeDto(
    val name: String = "",
    val capacity: Int = 0,
    val baseRate: Long = 0,
    val taxId: String? = null,
    val active: Boolean = true,
)

internal fun RoomType.toDto() =
    RoomTypeDto(name = name, capacity = capacity, baseRate = baseRate, taxId = taxId, active = active)

internal fun RoomTypeDto.toRoomType(id: String) =
    RoomType(id = id, name = name, capacity = capacity, baseRate = baseRate, taxId = taxId, active = active)

/**
 * A room stored at `Rooms/{id}` (RF-HAB-02), with its state in parts (section 6.8). The expected
 * check-out is a day of Colombia, written as `2026-11-22`.
 */
@Serializable
internal data class RoomDto(
    val number: Int = 0,
    val roomTypeId: String = "",
    val floor: Int = 0,
    val occupancy: String = VACANT,
    val housekeeping: String = DIRTY,
    val expectedCheckOutDate: String? = null,
    val active: Boolean = true,
    val housekeepingChangedBy: String? = null,
    val housekeepingChangedAt: BaseTimestamp? = null,
)

internal fun Room.toDto() = RoomDto(
    number = number,
    roomTypeId = roomTypeId,
    floor = floor,
    occupancy = occupancy.code(),
    housekeeping = housekeeping.code(),
    expectedCheckOutDate = expectedCheckOutDate?.isoText,
    active = active,
)

internal fun RoomDto.toRoom(id: String) = Room(
    id = id,
    number = number,
    roomTypeId = roomTypeId,
    floor = floor,
    occupancy = if (occupancy == OCCUPIED) Occupancy.OCCUPIED else Occupancy.VACANT,
    housekeeping = housekeepingOf(housekeeping),
    expectedCheckOutDate = expectedCheckOutDate?.let(CalendarDate::parse),
    active = active,
    housekeepingChangedBy = housekeepingChangedBy,
    housekeepingChangedAt = (housekeepingChangedAt as? Timestamp)?.toInstant(),
)

internal fun Occupancy.code(): String = when (this) {
    Occupancy.VACANT -> VACANT
    Occupancy.OCCUPIED -> OCCUPIED
}

internal fun Housekeeping.code(): String = when (this) {
    Housekeeping.DIRTY -> DIRTY
    Housekeeping.CLEANING -> CLEANING
    Housekeeping.CLEAN -> CLEAN
}

// A value the app does not know reads as to be cleaned, so the board never offers it as available.
private fun housekeepingOf(code: String): Housekeeping = when (code) {
    CLEAN -> Housekeeping.CLEAN
    CLEANING -> Housekeeping.CLEANING
    else -> Housekeeping.DIRTY
}

private const val VACANT = "vacant"
private const val OCCUPIED = "occupied"
private const val DIRTY = "dirty"
private const val CLEANING = "cleaning"
private const val CLEAN = "clean"
