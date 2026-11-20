package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveMyScheduleUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeShiftRepository
import com.peakdevcol.project.hotelmanagement.fakes.shift
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceIntent
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** RF-ASI-02. */
class AttendanceViewModelTest : MainDispatcherTest() {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-20T15:00:00Z")
    }
    private val shifts = FakeShiftRepository(
        shift("today", "ana", "2026-11-20", "06:00", "14:00"),
        shift("tomorrow-night", "ana", "2026-11-21", "22:00", "06:00"),
        shift("tomorrow", "ana", "2026-11-21", "06:00", "14:00"),
        shift("not-mine", "luis", "2026-11-21", "06:00", "14:00"),
    )

    private fun viewModel() = AttendanceViewModel(ObserveMyScheduleUseCase(FakeAuthRepository(userId = "ana"), shifts, clock), clock)

    @Test
    fun showsMyShiftsByDayAndKnowsWhichDayIsToday() {
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals(CalendarDate(2026, 11, 20), state.today)
        assertEquals(listOf(CalendarDate(2026, 11, 20), CalendarDate(2026, 11, 21)), state.days.map { it.date })
        assertEquals(listOf("tomorrow", "tomorrow-night"), state.days.last().shifts.map { it.id })
    }

    @Test
    fun aNewShiftAppearsWithoutReloading() {
        val viewModel = viewModel()

        shifts.shifts.value += shift("added", "ana", "2026-11-25", "06:00", "14:00")

        assertEquals(3, viewModel.state.value.days.size)
    }

    @Test
    fun showsAnErrorAndRetries() {
        shifts.readFailure = IllegalStateException("permission denied")
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.loadFailed)

        shifts.readFailure = null
        viewModel.onIntent(AttendanceIntent.Retry)

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(2, viewModel.state.value.days.size)
    }
}
