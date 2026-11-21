package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkWithPhoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveMyScheduleUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntryUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeLocationProvider
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeShiftRepository
import com.peakdevcol.project.hotelmanagement.fakes.entry
import com.peakdevcol.project.hotelmanagement.fakes.hotelSettings
import com.peakdevcol.project.hotelmanagement.fakes.readingNorthOfHotel
import com.peakdevcol.project.hotelmanagement.fakes.shift
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceIntent
import com.peakdevcol.project.hotelmanagement.ui.attendance.AttendanceViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendance.MarkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** RF-ASI-02 to RF-ASI-05 and S10. */
class AttendanceViewModelTest : MainDispatcherTest() {
    private val now = Instant.parse("2026-11-20T15:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val minuteTicks = MutableStateFlow(now)
    private val auth = FakeAuthRepository(userId = "ana")
    private val shifts = FakeShiftRepository(
        shift("today", "ana", "2026-11-20", "06:00", "14:00"),
        shift("tomorrow-night", "ana", "2026-11-21", "22:00", "06:00"),
        shift("tomorrow", "ana", "2026-11-21", "06:00", "14:00"),
        shift("not-mine", "luis", "2026-11-21", "06:00", "14:00"),
    )
    private val attendance = FakeAttendanceRepository()
    private val settings = FakeSettingsRepository(hotelSettings)
    private val location = FakeLocationProvider()

    private fun viewModel(location: FakeLocationProvider = this.location) = AttendanceViewModel(
        observeMySchedule = ObserveMyScheduleUseCase(auth, shifts, clock),
        observeOpenEntry = ObserveOpenEntryUseCase(auth, attendance, settings),
        markWithPhone = MarkWithPhoneUseCase(auth, attendance, settings, location, clock),
        locationAccess = LocationAccessUseCase(location),
        clock = clock,
        minuteTicks = minuteTicks,
    )

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

    @Test
    fun withoutAnOpenEntryTheNextMarkIsTheEntry() {
        val marking = viewModel().state.value.marking!!

        assertFalse(marking.isLoading)
        assertNull(marking.currentEntry)
        assertEquals(MarkKind.ENTRY, marking.nextMark)
    }

    @Test
    fun markingTheEntryShowsItAndTheButtonTurnsIntoTheExit() {
        val viewModel = viewModel()

        viewModel.onIntent(AttendanceIntent.Mark)

        val marking = viewModel.state.value.marking!!
        val result = assertIs<MarkResult.Done>(marking.result)
        assertEquals(MarkKind.ENTRY, result.outcome.kind)
        assertEquals(MarkKind.EXIT, marking.nextMark)
        assertFalse(marking.isMarking)
    }

    @Test
    fun theTimeWorkedSoFarFollowsTheClock() {
        attendance.records.value = listOf(entry("a-1", "ana", at = "2026-11-20T11:00:00Z"))
        val viewModel = viewModel()

        minuteTicks.value = Instant.parse("2026-11-20T15:30:00Z")

        val marking = viewModel.state.value.marking!!
        assertEquals("a-1", marking.currentEntry?.id)
        assertEquals(4.5.hours, marking.now - marking.currentEntry!!.clockIn.at!!)
    }

    @Test
    fun anEntryOlderThanTheParameterIsForgottenAndTheNextMarkIsAnEntry() {
        attendance.records.value = listOf(entry("yesterday", "ana", at = "2026-11-19T11:00:00Z"))

        val marking = viewModel().state.value.marking!!

        assertNull(marking.currentEntry)
        assertEquals("yesterday", marking.forgottenEntry?.id)
        assertEquals(MarkKind.ENTRY, marking.nextMark)
    }

    @Test
    fun outsideTheZoneTheMarkFailsWithTheWayOut() {
        location.result = LocationResult.Found(readingNorthOfHotel(degrees = 0.0045))
        val viewModel = viewModel()

        viewModel.onIntent(AttendanceIntent.Mark)

        assertEquals(MarkResult.Failed(MarkKind.ENTRY, AttendanceError.OutsideZone(500)), viewModel.state.value.marking?.result)
        assertTrue(attendance.records.value.isEmpty())
    }

    @Test
    fun withoutPermissionTheAppExplainsBeforeTheSystemAsks() {
        location.permission = false
        val viewModel = viewModel()

        viewModel.onIntent(AttendanceIntent.Mark)
        assertTrue(viewModel.state.value.isExplainingPermission)
        assertTrue(location.requests.isEmpty())

        location.permission = true
        viewModel.onIntent(AttendanceIntent.PermissionResult(granted = true))

        assertFalse(viewModel.state.value.isExplainingPermission)
        assertIs<MarkResult.Done>(viewModel.state.value.marking?.result)
    }

    @Test
    fun aDeniedPermissionSaysToMarkAtReception() {
        location.permission = false
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceIntent.Mark)

        viewModel.onIntent(AttendanceIntent.PermissionResult(granted = false))

        assertEquals(MarkResult.Failed(MarkKind.ENTRY, AttendanceError.LocationPermissionDenied), viewModel.state.value.marking?.result)
    }

    @Test
    fun closingTheExplanationMarksNothing() {
        location.permission = false
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceIntent.Mark)

        viewModel.onIntent(AttendanceIntent.DismissPermission)

        assertFalse(viewModel.state.value.isExplainingPermission)
        assertNull(viewModel.state.value.marking?.result)
    }

    @Test
    fun withoutLocationOnThisDeviceThereIsNoMarkingFromIt() {
        val state = viewModel(FakeLocationProvider(isAvailable = false)).state.value

        assertNull(state.marking)
    }
}
