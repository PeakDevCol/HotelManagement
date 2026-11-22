package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.model.startOfOperationalDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class OperationalDayTest {
    private val sixAm = 6 * 60

    @Test
    fun beforeTheStartOfTheDayASaleStillBelongsToTheDayBefore() {
        // 5:59 a. m. and 6:00 a. m. of 17 nov in Colombia.
        assertEquals(CalendarDate(2026, 11, 16), operationalDateOf(Instant.parse("2026-11-17T10:59:00Z"), sixAm))
        assertEquals(CalendarDate(2026, 11, 17), operationalDateOf(Instant.parse("2026-11-17T11:00:00Z"), sixAm))
    }

    @Test
    fun withTheDayStartingAtMidnightItIsTheColombianDate() {
        assertEquals(CalendarDate(2026, 12, 31), operationalDateOf(Instant.parse("2027-01-01T04:59:00Z"), 0))
    }

    @Test
    fun anOperationalDayStartsAtItsHourInColombia() {
        assertEquals(Instant.parse("2026-11-17T11:00:00Z"), startOfOperationalDay(CalendarDate(2026, 11, 17), sixAm))
    }

    @Test
    fun knowsTheLastDayOfEachMonth() {
        assertEquals(CalendarDate(2028, 2, 29), CalendarDate(2028, 2, 10).lastDayOfMonth)
        assertEquals(CalendarDate(2027, 2, 28), CalendarDate(2027, 2, 10).lastDayOfMonth)
        assertEquals(CalendarDate(2026, 12, 31), CalendarDate(2026, 12, 1).lastDayOfMonth)
    }

    @Test
    fun readsOnlyRealDates() {
        assertEquals(CalendarDate(2026, 11, 1), CalendarDate.parse("2026-11-01"))
        assertNull(CalendarDate.parse("2026-02-30"))
        assertNull(CalendarDate.parse("2026-11"))
    }

    @Test
    fun aDayIsWrittenAsItIsReadBack() {
        assertEquals("2026-03-05", CalendarDate(2026, 3, 5).isoText)
        assertEquals(CalendarDate(2026, 12, 31), CalendarDate.parse(CalendarDate(2026, 12, 31).isoText))
    }

    @Test
    fun formatsADayTheWayTheHotelReadsIt() {
        assertEquals("1 nov 2026", formatCalendarDate(CalendarDate(2026, 11, 1)))
    }

    @Test
    fun theFirstHoursOfTheMonthBelongToTheMonthBefore() {
        // 3:00 a. m. of 1 dec in Colombia is still the operational day of 30 nov.
        val period = BalancePeriod.monthOf(Instant.parse("2026-12-01T08:00:00Z"), sixAm)

        assertEquals(BalancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)), period)
        assertEquals(Instant.parse("2026-11-01T11:00:00Z"), period.startsAt(sixAm))
        assertEquals(Instant.parse("2026-12-01T11:00:00Z"), period.endsAt(sixAm))
    }
}
