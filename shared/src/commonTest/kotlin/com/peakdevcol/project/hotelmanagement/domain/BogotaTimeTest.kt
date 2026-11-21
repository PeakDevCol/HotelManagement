package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatDayRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BogotaTimeTest {
    private fun format(utc: String) = formatBogotaDateTime(Instant.parse(utc))

    @Test
    fun showsTheTimeOfColombiaFiveHoursBehindUtc() {
        assertEquals("14 nov 2026, 7:23 p. m.", format("2026-11-15T00:23:00Z"))
    }

    @Test
    fun morningHoursAreAm() {
        assertEquals("15 nov 2026, 6:05 a. m.", format("2026-11-15T11:05:00Z"))
    }

    @Test
    fun midnightAndNoonAreTwelve() {
        assertEquals("15 nov 2026, 12:00 a. m.", format("2026-11-15T05:00:00Z"))
        assertEquals("15 nov 2026, 12:30 p. m.", format("2026-11-15T17:30:00Z"))
    }

    @Test
    fun theDayChangesWithTheColombianDateNotTheUtcOne() {
        assertEquals("31 dic 2026, 11:59 p. m.", format("2027-01-01T04:59:00Z"))
        assertEquals("1 ene 2027, 12:00 a. m.", format("2027-01-01T05:00:00Z"))
    }

    @Test
    fun handlesLeapDays() {
        assertEquals("29 feb 2028, 3:00 p. m.", format("2028-02-29T20:00:00Z"))
        assertEquals("1 mar 2027, 9:10 a. m.", format("2027-03-01T14:10:00Z"))
    }

    @Test
    fun aPeriodDoesNotRepeatTheMonthOrTheYearItShares() {
        assertEquals("16 – 30 nov 2026", formatDayRange(CalendarDate(2026, 11, 16), CalendarDate(2026, 11, 30)))
        assertEquals("16 nov – 15 dic 2026", formatDayRange(CalendarDate(2026, 11, 16), CalendarDate(2026, 12, 15)))
        assertEquals("16 dic 2026 – 15 ene 2027", formatDayRange(CalendarDate(2026, 12, 16), CalendarDate(2027, 1, 15)))
    }
}
