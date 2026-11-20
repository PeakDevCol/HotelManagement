package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftDraft
import com.peakdevcol.project.hotelmanagement.domain.model.byStartDay
import com.peakdevcol.project.hotelmanagement.domain.model.formatClockTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.overlappingIds
import com.peakdevcol.project.hotelmanagement.domain.model.parseClockTime
import com.peakdevcol.project.hotelmanagement.domain.model.weekStartOf
import com.peakdevcol.project.hotelmanagement.fakes.shift
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** RF-ASI-01 and RF-ASI-02. */
class ShiftTest {
    private val friday = CalendarDate(2026, 11, 20)

    @Test
    fun aShiftIsWrittenInColombianTime() {
        val morning = ShiftDraft("ana", friday, startMinutes = 6 * 60, endMinutes = 14 * 60).toShift("s").getOrThrow()

        assertEquals(Instant.parse("2026-11-20T11:00:00Z"), morning.startAt)
        assertEquals(8.hours, morning.endAt - morning.startAt)
    }

    @Test
    fun anEndBeforeTheStartEndsTheNextDay() {
        val night = ShiftDraft("ana", friday, startMinutes = 22 * 60, endMinutes = 6 * 60).toShift("s").getOrThrow()

        assertEquals(Instant.parse("2026-11-21T11:00:00Z"), night.endAt)
        assertEquals(8.hours, night.endAt - night.startAt)
    }

    @Test
    fun aShiftNeedsAnEmployeeAndTwoDifferentTimes() {
        assertEquals(ScheduleError.MissingEmployee, ShiftDraft(null, friday, 360, 840).toShift("s").exceptionOrNull())
        assertEquals(ScheduleError.InvalidTime, ShiftDraft("ana", friday, null, 840).toShift("s").exceptionOrNull())
        assertEquals(ScheduleError.EmptyShift, ShiftDraft("ana", friday, 360, 360).toShift("s").exceptionOrNull())
    }

    @Test
    fun aShiftGoesBackToItsDraftToBeChanged() {
        val night = shift("s", "ana", "2026-11-20", "22:00", "06:00")

        assertEquals(ShiftDraft("ana", friday, 22 * 60, 6 * 60), ShiftDraft.of(night))
    }

    @Test
    fun onlyShiftsOfTheSameEmployeeThatShareTimeOverlap() {
        val shifts = listOf(
            shift("morning", "ana", "2026-11-20", "06:00", "14:00"),
            shift("afternoon", "ana", "2026-11-20", "13:00", "21:00"),
            shift("back-to-back", "ana", "2026-11-20", "21:00", "23:00"),
            shift("other", "luis", "2026-11-20", "06:00", "14:00"),
        )

        assertEquals(setOf("morning", "afternoon"), shifts.overlappingIds())
    }

    @Test
    fun aNightShiftOverlapsTheMorningOfTheNextDay() {
        val shifts = listOf(shift("night", "ana", "2026-11-20", "22:00", "07:00"), shift("morning", "ana", "2026-11-21", "06:00", "14:00"))

        assertEquals(setOf("night", "morning"), shifts.overlappingIds())
    }

    @Test
    fun weeksGoFromMondayToSunday() {
        assertEquals(CalendarDate(2026, 11, 16), weekStartOf(friday))
        assertEquals(CalendarDate(2026, 11, 16), weekStartOf(CalendarDate(2026, 11, 16)))
        assertEquals(CalendarDate(2026, 11, 16), weekStartOf(CalendarDate(2026, 11, 22)))
        assertEquals("vie 20 nov", formatWeekday(friday))
        assertEquals("dom 22 nov", formatWeekday(CalendarDate(2026, 11, 22)))
    }

    @Test
    fun readsTimesWithOrWithoutTheColon() {
        assertEquals(6 * 60, parseClockTime("6"))
        assertEquals(18 * 60 + 30, parseClockTime("18:30"))
        assertEquals(6 * 60 + 30, parseClockTime("630"))
        assertEquals(18 * 60 + 30, parseClockTime(" 1830 "))
        assertNull(parseClockTime("24:00"))
        assertNull(parseClockTime("18:3"))
        assertNull(parseClockTime("6:60"))
        assertNull(parseClockTime(""))
        assertEquals("06:05", formatClockTime(6 * 60 + 5))
    }

    @Test
    fun myShiftsAreGroupedByTheDayTheyStart() {
        val days = listOf(
            shift("b", "ana", "2026-11-21", "06:00", "14:00"),
            shift("a", "ana", "2026-11-20", "22:00", "06:00"),
        ).byStartDay()

        assertEquals(listOf(friday, CalendarDate(2026, 11, 21)), days.map { it.date })
        assertEquals(listOf("a"), days.first().shifts.map { it.id })
    }
}
