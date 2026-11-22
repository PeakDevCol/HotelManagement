package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.byFloor
import com.peakdevcol.project.hotelmanagement.domain.model.markableAs
import com.peakdevcol.project.hotelmanagement.domain.model.status
import com.peakdevcol.project.hotelmanagement.fakes.room
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** RF-HAB-03 and RF-HAB-04, with decisions 4A and 5A. */
class RoomTest {
    private val free = room("r-204", 204)

    @Test
    fun aNewRoomIsAvailable() {
        assertEquals(Occupancy.VACANT, free.occupancy)
        assertEquals(Housekeeping.CLEAN, free.housekeeping)
        assertEquals(RoomStatus.AVAILABLE, free.status)
    }

    @Test
    fun theBoardShowsOneStateWithTheOccupancyFirst() {
        assertEquals(RoomStatus.OCCUPIED, free.copy(occupancy = Occupancy.OCCUPIED, housekeeping = Housekeeping.DIRTY).status)
        assertEquals(RoomStatus.CLEANING, free.copy(housekeeping = Housekeeping.CLEANING).status)
        assertEquals(RoomStatus.DIRTY, free.copy(housekeeping = Housekeeping.DIRTY).status)
    }

    @Test
    fun aFreeRoomIsMarkedCleanOrBackToBeCleaned() {
        assertEquals(Housekeeping.CLEAN, free.copy(housekeeping = Housekeeping.DIRTY).markableAs)
        assertEquals(Housekeeping.DIRTY, free.markableAs)
    }

    @Test
    fun anOccupiedOrInactiveRoomOrOneBeingCleanedIsNotMarkedByHand() {
        assertNull(free.copy(occupancy = Occupancy.OCCUPIED, housekeeping = Housekeeping.DIRTY).markableAs)
        assertNull(free.copy(active = false, housekeeping = Housekeeping.DIRTY).markableAs)
        assertNull(free.copy(housekeeping = Housekeeping.CLEANING).markableAs)
    }

    @Test
    fun theFloorsGoFromTheLowestWithTheirRoomsByNumber() {
        val rooms = listOf(room("a", 302), room("b", 101), room("c", 301), room("d", 1)).map { BoardRoom(it, type = null) }

        val floors = rooms.byFloor()

        assertEquals(listOf(0, 1, 3), floors.map { it.floor })
        assertEquals(listOf(301, 302), floors.last().rooms.map { it.room.number })
    }
}
