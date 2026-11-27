package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyPeriod
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancySpan
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOccupancyUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeReservationRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomTypeRepository
import com.peakdevcol.project.hotelmanagement.fakes.day
import com.peakdevcol.project.hotelmanagement.fakes.reservation
import com.peakdevcol.project.hotelmanagement.fakes.room
import com.peakdevcol.project.hotelmanagement.fakes.roomType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** RF-RES-04, with decision 2A of the third PR of M11 and decision 4A. */
class OccupancyTest {
    private val twoWeeks = OccupancyPeriod.from(day("2026-11-22"))

    @Test
    fun twoWeeksGoFourteenDaysAtATime() {
        assertEquals(day("2026-12-05"), twoWeeks.lastDay)
        assertEquals(14, twoWeeks.days.size)
        assertEquals(day("2026-12-06"), twoWeeks.next().firstDay)
        assertEquals(day("2026-11-08"), twoWeeks.previous().firstDay)
    }

    @Test
    fun aMonthIsTheWholeCalendarMonth() {
        val february = OccupancyPeriod.from(day("2027-02-14"), OccupancySpan.MONTH)

        assertEquals(day("2027-02-01"), february.firstDay)
        assertEquals(day("2027-02-28"), february.lastDay)
        assertEquals(day("2027-03-01"), february.next().firstDay)
        assertEquals(day("2027-01-01"), february.previous().firstDay)
    }

    @Test
    fun changingTheSpanKeepsTheMomentInSight() {
        val today = day("2026-11-22")

        assertEquals(OccupancyPeriod(OccupancySpan.MONTH, day("2026-11-01")), twoWeeks.inSpan(OccupancySpan.MONTH, today))
        assertEquals(twoWeeks, twoWeeks.inSpan(OccupancySpan.MONTH, today).inSpan(OccupancySpan.TWO_WEEKS, today), "today is in that month")
        assertEquals(
            OccupancyPeriod(OccupancySpan.TWO_WEEKS, day("2027-01-01")),
            OccupancyPeriod(OccupancySpan.MONTH, day("2027-01-01")).inSpan(OccupancySpan.TWO_WEEKS, today),
        )
    }

    @Test
    fun theCalendarHasTheRoomsByFloorWithTheStaysThatTouchThePeriod() = runTest {
        val rooms = FakeRoomRepository(room("r-204", 204), room("r-101", 101), room("r-305", 305).copy(active = false), room("r-306", 306).copy(active = false))
        val reservations = FakeReservationRepository(
            // Arrived 20 days before the period and still holds its first nights.
            reservation("long", roomId = "r-204", checkIn = "2026-11-02", checkOut = "2026-11-25", roomNumber = 204),
            reservation("later", roomId = "r-204", checkIn = "2026-11-28", checkOut = "2026-11-30", roomNumber = 204),
            reservation("before", roomId = "r-101", checkIn = "2026-11-10", checkOut = "2026-11-22", roomNumber = 101),
            reservation("after", roomId = "r-101", checkIn = "2026-12-06", checkOut = "2026-12-08", roomNumber = 101),
            reservation("cancelled", roomId = "r-101", checkIn = "2026-11-23", checkOut = "2026-11-24", roomNumber = 101).copy(state = ReservationState.CANCELLED),
            reservation("in-house", roomId = "r-305", checkIn = "2026-11-21", checkOut = "2026-11-23", roomNumber = 305).copy(state = ReservationState.CHECKED_IN),
        )

        val calendar = ObserveOccupancyUseCase(reservations, rooms, FakeRoomTypeRepository(roomType("double", "Doble")))(twoWeeks).first()

        assertEquals(listOf(101, 204, 305), calendar.rooms.map { it.room.number }, "an inactive room shows only while a stay holds it")
        assertEquals(listOf("long", "later"), calendar.rooms.first { it.room.number == 204 }.reservations.map { it.id })
        assertEquals(emptyList(), calendar.rooms.first { it.room.number == 101 }.reservations, "it left the morning the period starts")
        assertEquals("Doble", calendar.rooms.first().typeName)
    }
}
