package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceLine
import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.PeriodPreset
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CorrectMarkUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadAttendanceReportUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntriesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterMissingMarksUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAttendanceCorrectionRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeShiftRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeTeamAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.bogotaInstant
import com.peakdevcol.project.hotelmanagement.fakes.bogotaRecord
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import com.peakdevcol.project.hotelmanagement.fakes.shift
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.AttendanceReviewEffect
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.AttendanceReviewIntent
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.AttendanceReviewViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.EditorForm
import com.peakdevcol.project.hotelmanagement.ui.attendancereview.ReviewTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** RF-ASI-05 to RF-ASI-08, with decisions 2A, 4A, 6A and 7A. */
class AttendanceReviewViewModelTest : MainDispatcherTest() {
    private fun bogota(text: String) = Instant.parse(bogotaInstant(text))

    // Saturday 2026-11-21: the fortnight goes from the 16th to the 30th.
    private val now = bogota("2026-11-21 10:00")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val minuteTicks = MutableStateFlow(now)

    // Marked at 7:02 on a phone without connection; it reached the server at 9:15.
    private val underReview = AttendanceRecord(
        id = "a-1",
        employeeId = "ana",
        clockIn = phoneMark(at = bogotaInstant("2026-11-20 09:15"), deviceTime = bogotaInstant("2026-11-20 07:02")),
        clockOut = phoneMark(at = bogotaInstant("2026-11-20 14:01")),
    )
    private val forgotten = bogotaRecord("a-2", "luis", "2026-11-19 06:00")

    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(employee("ana", "Ana Rojas"), employee("luis", "Luis Pérez"), employee("admin", "Laura Méndez", UserType.ADMIN))
    private val attendance = FakeAttendanceRepository(underReview, forgotten)
    private val team = FakeTeamAttendanceRepository(attendance)
    private val corrections = FakeAttendanceCorrectionRepository(attendance)
    private val shifts = FakeShiftRepository(
        shift("ana-fri", "ana", "2026-11-20", "06:00", "14:00"),
        shift("luis-thu", "luis", "2026-11-19", "06:00", "14:00"),
        shift("ana-wed", "ana", "2026-11-18", "06:00", "14:00"),
    )

    private fun viewModel() = AttendanceReviewViewModel(
        loadAttendanceReport = LoadAttendanceReportUseCase(shifts, team, FakeSettingsRepository()),
        observeOpenEntries = ObserveOpenEntriesUseCase(team),
        observeEmployees = ObserveEmployeesUseCase(users),
        correctMark = CorrectMarkUseCase(auth, corrections, clock),
        registerMissingMarks = RegisterMissingMarksUseCase(auth, corrections, clock),
        clock = clock,
        minuteTicks = minuteTicks,
    )

    @Test
    fun opensOnTheFortnightWithWhatWaitsForTheAdministrator() {
        val state = viewModel().state.value

        assertEquals(AttendancePeriod(CalendarDate(2026, 11, 16), CalendarDate(2026, 11, 30)), state.period)
        assertEquals(PeriodPreset.THIS_FORTNIGHT, state.preset)
        assertFalse(state.isLoading)
        assertEquals(now, state.loadedAt)
        assertEquals(listOf("a-2"), state.pendingExits.map { it.id })
        assertEquals(listOf("a-1"), state.report?.toConfirm?.map { it.id })
        assertEquals(2, state.toReviewCount)
        assertEquals(listOf("ana", "luis"), state.employeeHours.map { it.employeeId })
    }

    @Test
    fun anotherPeriodIsReadAgain() {
        val viewModel = viewModel()

        viewModel.onIntent(AttendanceReviewIntent.SelectPreset(PeriodPreset.LAST_FORTNIGHT))
        viewModel.onIntent(AttendanceReviewIntent.SelectTab(ReviewTab.HOURS))

        assertEquals(AttendancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 15)), viewModel.state.value.period)
        assertEquals(2, team.reads.size)
        assertEquals(ReviewTab.HOURS, viewModel.state.value.tab)
        assertTrue(viewModel.state.value.report?.toConfirm.orEmpty().isEmpty())
    }

    @Test
    fun daysChosenByHandMustNotPassAMonth() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.PickDays)
        assertTrue(viewModel.state.value.isPickingDays)

        viewModel.onIntent(AttendanceReviewIntent.DaysPicked(CalendarDate(2026, 11, 1).epochDay, CalendarDate(2026, 12, 5).epochDay))

        assertEquals(AttendanceReviewEffect.PeriodRejected(ReportError.PeriodTooLong), viewModel.effects.first())
        assertFalse(viewModel.state.value.isPickingDays)
        assertEquals(PeriodPreset.THIS_FORTNIGHT, viewModel.state.value.preset)

        viewModel.onIntent(AttendanceReviewIntent.DaysPicked(CalendarDate(2026, 11, 19).epochDay, CalendarDate(2026, 11, 20).epochDay))
        assertNull(viewModel.state.value.preset)
        assertEquals(CalendarDate(2026, 11, 19), viewModel.state.value.period.firstDay)
    }

    @Test
    fun withOneTapTheTimeOfTheServerIsSavedAndTheSheetCloses() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.OpenRecord("a-1"))
        assertEquals("ana-fri", viewModel.state.value.editor?.shift?.id)

        viewModel.onIntent(AttendanceReviewIntent.UseServerTime(MarkKind.ENTRY))

        assertEquals(AttendanceReviewEffect.Corrected, viewModel.effects.first())
        val state = viewModel.state.value
        assertNull(state.editor)
        assertTrue(state.report?.toConfirm.orEmpty().isEmpty())
        assertEquals(bogota("2026-11-20 09:15"), state.data?.records?.single { it.id == "a-1" }?.entryTime)
        assertEquals(CorrectionKind.SERVER_TIME, corrections.corrections.single().kind)
    }

    @Test
    fun anotherTimeNeedsAReasonAndTheSheetStaysOpenUntilItHasOne() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.OpenRecord("a-1"))
        viewModel.onIntent(AttendanceReviewIntent.EditTime(MarkKind.ENTRY))
        assertEquals(EditorForm.MarkTime(MarkKind.ENTRY, "07:02"), viewModel.state.value.editor?.form)

        viewModel.onIntent(AttendanceReviewIntent.TimeChange("0700"))
        viewModel.onIntent(AttendanceReviewIntent.Save)
        assertEquals(CorrectionError.MissingReason, viewModel.state.value.editor?.error)

        viewModel.onIntent(AttendanceReviewIntent.ReasonChange("Llegó con el turno"))
        assertNull(viewModel.state.value.editor?.error)
        assertEquals(bogota("2026-11-20 07:00"), viewModel.state.value.editor?.typedTime)
        viewModel.onIntent(AttendanceReviewIntent.Save)

        assertEquals(AttendanceReviewEffect.Corrected, viewModel.effects.first())
        assertEquals("Llegó con el turno", corrections.corrections.single().reason)
        assertEquals(bogota("2026-11-20 07:00"), viewModel.state.value.data?.records?.single { it.id == "a-1" }?.entryTime)
    }

    @Test
    fun aTimeThatIsNotATimeOfTheDayIsShownInTheForm() {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.OpenRecord("a-1"))
        viewModel.onIntent(AttendanceReviewIntent.EditTime(MarkKind.EXIT))
        viewModel.onIntent(AttendanceReviewIntent.TimeChange("25:00"))
        viewModel.onIntent(AttendanceReviewIntent.ReasonChange("Error"))

        viewModel.onIntent(AttendanceReviewIntent.Save)

        assertEquals(CorrectionError.InvalidTime, viewModel.state.value.editor?.error)
        assertTrue(corrections.corrections.isEmpty())
    }

    @Test
    fun theExitOfAForgottenEntryStartsAtTheEndOfItsShiftAndClosesIt() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.SetMissingExit("a-2"))
        assertEquals(EditorForm.MarkTime(MarkKind.EXIT, "14:00"), viewModel.state.value.editor?.form)
        assertEquals(bogota("2026-11-19 14:00"), viewModel.state.value.editor?.typedTime)

        viewModel.onIntent(AttendanceReviewIntent.ReasonChange("Se le olvidó marcar"))
        viewModel.onIntent(AttendanceReviewIntent.Save)

        assertEquals(AttendanceReviewEffect.Corrected, viewModel.effects.first())
        val state = viewModel.state.value
        assertTrue(state.pendingExits.isEmpty())
        assertEquals(MarkSource.ADMIN, state.data?.records?.single { it.id == "a-2" }?.clockOut?.source)
        // Only the mark to confirm is left.
        assertEquals(1, state.toReviewCount)
    }

    @Test
    fun aShiftThatEndedWithoutMarksIsRegisteredWithItsTimes() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.RegisterShift("ana-wed"))
        val editor = assertNotNull(viewModel.state.value.editor)
        assertNull(editor.record)
        assertEquals(EditorForm.Registration("06:00", "14:00"), editor.form)

        viewModel.onIntent(AttendanceReviewIntent.ReasonChange("Marcó en la planilla de papel"))
        viewModel.onIntent(AttendanceReviewIntent.Save)

        assertEquals(AttendanceReviewEffect.Registered, viewModel.effects.first())
        val wednesday = viewModel.state.value.hoursOf("ana")?.lines?.filterIsInstance<AttendanceLine.Scheduled>()?.single { it.shift.id == "ana-wed" }
        assertEquals(false, wednesday?.isMissing)
        assertEquals(MarkSource.ADMIN, wednesday?.records?.single()?.record?.clockIn?.source)
    }

    @Test
    fun anErrorOfTheServerStaysInTheSheet() {
        corrections.failure = CorrectionError.Network
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.OpenRecord("a-1"))

        viewModel.onIntent(AttendanceReviewIntent.UseDeviceTime(MarkKind.ENTRY))

        val state = viewModel.state.value
        assertEquals(CorrectionError.Network, state.editor?.error)
        assertFalse(state.isSaving)
        assertEquals(1, state.report?.toConfirm?.size)
    }

    @Test
    fun cancellingTheFormGoesBackToTheRecordAndThenTheSheetCloses() {
        val viewModel = viewModel()
        viewModel.onIntent(AttendanceReviewIntent.OpenRecord("a-1"))
        viewModel.onIntent(AttendanceReviewIntent.EditTime(MarkKind.ENTRY))

        viewModel.onIntent(AttendanceReviewIntent.CancelForm)
        assertNull(viewModel.state.value.editor?.form)
        assertNotNull(viewModel.state.value.editor)

        viewModel.onIntent(AttendanceReviewIntent.DismissEditor)
        assertNull(viewModel.state.value.editor)
    }

    @Test
    fun aReadingThatFailsKeepsTheReportOfBeforeWithTheError() {
        val viewModel = viewModel()
        team.readFailure = ReportError.Network

        viewModel.onIntent(AttendanceReviewIntent.Reload)

        val state = viewModel.state.value
        assertEquals(ReportError.Network, state.loadError)
        assertNotNull(state.report)
        assertFalse(state.isLoading)
    }

    @Test
    fun anEntryBecomesAPendingExitWhileTheScreenIsOpen() {
        attendance.records.value += bogotaRecord("a-3", "ana", "2026-11-21 06:00")
        val viewModel = viewModel()
        assertEquals(listOf("a-2"), viewModel.state.value.pendingExits.map { it.id })

        minuteTicks.value = bogota("2026-11-21 22:00")

        assertEquals(listOf("a-2", "a-3"), viewModel.state.value.pendingExits.map { it.id })
        val line = viewModel.state.value.hoursOf("ana")?.lines?.filterIsInstance<AttendanceLine.Unscheduled>()?.single()
        assertIs<AttendanceLine.Unscheduled>(line)
    }
}
