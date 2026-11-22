package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.RoomDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.RoomTypeDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRoom
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRoomType
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The fields and values of section 8 of the PRD, which the rules check. */
class RoomDtoTest {
    @Test
    fun aTypeIsReadBackAsItWasStored() {
        val type = RoomType(id = "double", name = "Doble", capacity = 2, baseRate = 250_000, taxId = "iva", active = false)

        assertEquals(RoomTypeDto(name = "Doble", capacity = 2, baseRate = 250_000, taxId = "iva", active = false), type.toDto())
        assertEquals(type, type.toDto().toRoomType("double"))
    }

    @Test
    fun aNewRoomIsStoredFreeAndCleanWithoutAMarkYet() {
        val dto = Room(id = "r-204", number = 204, roomTypeId = "double", floor = 2).toDto()

        assertEquals(RoomDto(number = 204, roomTypeId = "double", floor = 2, occupancy = "vacant", housekeeping = "clean", active = true), dto)
    }

    @Test
    fun theStateAndTheExpectedCheckOutAreReadBack() {
        val room = Room(
            id = "r-204",
            number = 204,
            roomTypeId = "double",
            floor = 2,
            occupancy = Occupancy.OCCUPIED,
            housekeeping = Housekeeping.CLEANING,
            expectedCheckOutDate = CalendarDate(2026, 11, 22),
        )

        assertEquals("2026-11-22", room.toDto().expectedCheckOutDate)
        assertEquals(room, room.toDto().toRoom("r-204"))
    }

    @Test
    fun whoMarkedTheHousekeepingIsReadWithTheTimeOfTheServer() {
        val dto = RoomDto(number = 204, roomTypeId = "double", floor = 2, occupancy = "vacant", housekeeping = "dirty", housekeepingChangedBy = "ana", housekeepingChangedAt = Timestamp(1_795_000_000, 0))

        val room = dto.toRoom("r-204")

        assertEquals(Housekeeping.DIRTY, room.housekeeping)
        assertEquals("ana", room.housekeepingChangedBy)
        assertEquals(Instant.fromEpochSeconds(1_795_000_000), room.housekeepingChangedAt)
    }

    @Test
    fun anUnknownHousekeepingReadsAsToBeCleaned() {
        assertEquals(Housekeeping.DIRTY, RoomDto(housekeeping = "inspected").toRoom("r").housekeeping)
    }
}
