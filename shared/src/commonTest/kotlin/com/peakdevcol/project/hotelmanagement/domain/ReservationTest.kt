package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.StayPrice
import com.peakdevcol.project.hotelmanagement.domain.model.check
import com.peakdevcol.project.hotelmanagement.domain.model.checkPeople
import com.peakdevcol.project.hotelmanagement.fakes.day
import com.peakdevcol.project.hotelmanagement.fakes.reservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** RF-RES-02 and RF-RES-03, with decision 4A. */
class ReservationTest {
    private val today = day("2026-11-22")

    @Test
    fun theCheckOutDayIsNotANightOfTheStay() {
        val stay = Stay(day("2026-11-30"), day("2026-12-02"))

        assertEquals(2, stay.nightCount)
        assertEquals(listOf(day("2026-11-30"), day("2026-12-01")), stay.nights)
    }

    @Test
    fun aStayStartsTodayOrLaterAndLastsFromOneToThirtyNights() {
        assertNull(Stay(today, today.plusDays(1)).check(today))
        assertNull(Stay(today, today.plusDays(30)).check(today))
        assertEquals(ReservationError.CheckOutNotAfterCheckIn, Stay(today, today).check(today))
        assertEquals(ReservationError.TooManyNights, Stay(today, today.plusDays(31)).check(today))
        assertEquals(ReservationError.ArrivalInThePast, Stay(today.plusDays(-1), today.plusDays(1)).check(today))
    }

    @Test
    fun childrenCountForTheCapacityAndThereIsAlwaysAnAdult() {
        assertNull(checkPeople(adults = 1, children = 1, capacity = 2))
        assertEquals(ReservationError.OverCapacity, checkPeople(adults = 2, children = 1, capacity = 2))
        assertEquals(ReservationError.NoAdults, checkPeople(adults = 0, children = 2, capacity = 4))
    }

    @Test
    fun eachNightIsHeldWithTheRoomAndTheDayAsItsIdAndTheFirstOneIsTheArrival() {
        val nights = reservation("r1", roomId = "room-204", checkIn = "2026-11-30", checkOut = "2026-12-02").roomNights

        assertEquals(listOf("room-204_2026-11-30", "room-204_2026-12-01"), nights.map { it.id })
        assertEquals(listOf(true, false), nights.map { it.arrival })
    }

    @Test
    fun theTotalIncludesTheTaxOfTheType() {
        val price = StayPrice(nightlyRate = 119_000, nights = 2, taxRate = 1_900)

        assertEquals(238_000, price.total)
        assertEquals(38_000, price.taxAmount)
    }

    @Test
    fun theTextOfAReservationCarriesNoNameOfTheGuest() {
        assertFalse("Ana" in reservation("r1", roomId = "room-204", checkIn = "2026-11-30", checkOut = "2026-12-02").toString())
    }
}
