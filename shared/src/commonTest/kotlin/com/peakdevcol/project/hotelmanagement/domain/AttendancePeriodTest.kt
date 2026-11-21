package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.PeriodPreset
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.fortnightOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** RF-ASI-08 and decision 7A. */
class AttendancePeriodTest {
    private fun day(text: String) = CalendarDate.parse(text)!!
    private fun period(first: String, last: String) = AttendancePeriod(day(first), day(last))

    @Test
    fun aFortnightGoesFromTheFirstToTheFifteenthOrFromTheSixteenthToTheEndOfTheMonth() {
        assertEquals(period("2026-11-01", "2026-11-15"), fortnightOf(day("2026-11-15")))
        assertEquals(period("2026-11-16", "2026-11-30"), fortnightOf(day("2026-11-16")))
        assertEquals(period("2027-02-16", "2027-02-28"), fortnightOf(day("2027-02-20")))
        assertEquals(period("2026-12-16", "2026-12-31"), fortnightOf(day("2026-12-31")))
    }

    @Test
    fun thePreviousFortnightCanBeInTheMonthOrTheYearBefore() {
        assertEquals(period("2026-11-01", "2026-11-15"), PeriodPreset.LAST_FORTNIGHT.periodFor(day("2026-11-21")))
        assertEquals(period("2026-10-16", "2026-10-31"), PeriodPreset.LAST_FORTNIGHT.periodFor(day("2026-11-03")))
        assertEquals(period("2026-12-16", "2026-12-31"), PeriodPreset.LAST_FORTNIGHT.periodFor(day("2027-01-05")))
    }

    @Test
    fun theWeekGoesFromMondayToSundayAndTheMonthIsWhole() {
        // 2026-11-21 is a Saturday.
        assertEquals(period("2026-11-16", "2026-11-22"), PeriodPreset.THIS_WEEK.periodFor(day("2026-11-21")))
        assertEquals(period("2026-11-01", "2026-11-30"), PeriodPreset.THIS_MONTH.periodFor(day("2026-11-21")))
        assertEquals(period("2026-11-16", "2026-11-30"), PeriodPreset.THIS_FORTNIGHT.periodFor(day("2026-11-21")))
    }

    @Test
    fun thePeriodRunsFromMidnightOfTheFirstDayInColombiaToMidnightAfterTheLast() {
        val fortnight = period("2026-11-16", "2026-11-30")

        assertEquals(Instant.parse("2026-11-16T05:00:00Z"), fortnight.startsAt)
        assertEquals(Instant.parse("2026-12-01T05:00:00Z"), fortnight.endsAt)
        assertTrue(Instant.parse("2026-12-01T04:59:59Z") in fortnight)
        assertFalse(Instant.parse("2026-12-01T05:00:00Z") in fortnight)
        assertEquals(15, fortnight.dayCount)
    }

    @Test
    fun aPeriodChosenByHandHasAtMostAMonth() {
        assertEquals(period("2026-11-01", "2026-12-01"), AttendancePeriod.between(day("2026-11-01"), day("2026-12-01")).getOrThrow())
        assertEquals(ReportError.PeriodTooLong, AttendancePeriod.between(day("2026-11-01"), day("2026-12-02")).exceptionOrNull())
        assertEquals(ReportError.InvalidPeriod, AttendancePeriod.between(day("2026-11-10"), day("2026-11-09")).exceptionOrNull())
    }
}
