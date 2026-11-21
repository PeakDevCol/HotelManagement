package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftDraft
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.usecase.CopyPreviousWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteShiftUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveMyScheduleUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveScheduleWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveShiftUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeShiftRepository
import com.peakdevcol.project.hotelmanagement.fakes.shift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** RF-ASI-01 and RF-ASI-02. */
class ScheduleUseCasesTest {
    private val monday = CalendarDate(2026, 11, 16)
    private val admin = FakeAuthRepository(userId = "admin")
    private val shifts = FakeShiftRepository(
        shift("sunday-night", "ana", "2026-11-15", "22:00", "07:00"),
        shift("monday", "ana", "2026-11-16", "06:00", "14:00"),
        shift("friday", "luis", "2026-11-20", "14:00", "22:00", area = UserArea.RECEPTION),
        shift("next-monday", "ana", "2026-11-23", "06:00", "14:00"),
    )

    @Test
    fun theWeekHasItsSevenDaysWithTheShiftsThatStartOnEach() = runTest {
        val week = ObserveScheduleWeekUseCase(shifts)(monday).first()

        assertEquals(7, week.size)
        assertEquals(listOf("monday"), week[0].shifts.map { it.shift.id })
        assertEquals(listOf("friday"), week[4].shifts.map { it.shift.id })
        assertTrue(week.none { day -> day.shifts.any { it.shift.id == "next-monday" || it.shift.id == "sunday-night" } })
    }

    @Test
    fun aNightShiftOfTheSundayBeforeWarnsOnMonday() = runTest {
        val monday = ObserveScheduleWeekUseCase(shifts)(monday).first().first()

        assertTrue(monday.shifts.single().overlapsAnother)
    }

    @Test
    fun anEmployeeSeesTheirShiftsFromNowToTwoWeeksAheadAlsoTheOneGoingOn() = runTest {
        val clock = object : Clock {
            override fun now() = Instant.parse("2026-11-16T15:00:00Z")
        }
        shifts.shifts.value += shift("far", "ana", "2026-12-15", "06:00", "14:00")

        val mine = ObserveMyScheduleUseCase(FakeAuthRepository(userId = "ana"), shifts, clock)().first()

        assertEquals(listOf("monday", "next-monday"), mine.map { it.id })
    }

    @Test
    fun savesANewShiftSignedByTheAdministrator() = runTest {
        val result = SaveShiftUseCase(admin, shifts)(shiftId = null, ShiftDraft("luis", monday, 6 * 60, 14 * 60))

        assertTrue(result.isSuccess)
        assertEquals(5, shifts.shifts.value.size)
        assertEquals(listOf("admin"), shifts.savedBy)
    }

    @Test
    fun changesAnExistingShiftInsteadOfAddingAnother() = runTest {
        SaveShiftUseCase(admin, shifts)(shiftId = "monday", ShiftDraft("ana", monday, 7 * 60, 15 * 60))

        assertEquals(4, shifts.shifts.value.size)
        assertEquals(Instant.parse("2026-11-16T12:00:00Z"), shifts.shifts.value.first { it.id == "monday" }.startAt)
    }

    @Test
    fun aWrongShiftIsNotSaved() = runTest {
        val result = SaveShiftUseCase(admin, shifts)(shiftId = null, ShiftDraft(null, monday, 360, 840))

        assertEquals(ScheduleError.MissingEmployee, result.exceptionOrNull())
        assertTrue(shifts.savedBy.isEmpty())
    }

    @Test
    fun deletesAShift() = runTest {
        DeleteShiftUseCase(shifts)("friday")

        assertFalse(shifts.shifts.value.any { it.id == "friday" })
    }

    @Test
    fun copiesThePreviousWeekWithoutRepeatingWhatIsAlreadyThere() = runTest {
        val copy = CopyPreviousWeekUseCase(admin, shifts)

        val added = copy(CalendarDate(2026, 11, 23)).getOrThrow()

        // "monday" already has its copy, "next-monday"; the night of Sunday 15 belongs to the week before.
        assertEquals(1, added)
        val copied = shifts.shifts.value.single { it.employeeId == "luis" && it.startAt == Instant.parse("2026-11-27T19:00:00Z") }
        assertEquals(UserArea.RECEPTION, copied.area)
        assertEquals(0, copy(CalendarDate(2026, 11, 23)).getOrThrow())
    }

    @Test
    fun anEmptyWeekBeforeHasNothingToCopy() = runTest {
        val result = CopyPreviousWeekUseCase(admin, shifts)(CalendarDate(2026, 1, 5))

        assertEquals(ScheduleError.NothingToCopy, result.exceptionOrNull())
    }
}
