package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.MarkTimeChoice
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CorrectMarkUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadAttendanceReportUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveWhoIsWorkingUseCase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** RF-ASI-06 to RF-ASI-09. */
class AttendanceReviewUseCasesTest {
    private fun bogota(text: String) = Instant.parse(bogotaInstant(text))

    private val now = bogota("2026-11-21 10:00")
    private val clock = object : Clock {
        override fun now() = now
    }

    // Marked at 7:02 on a phone without connection; it reached the server at 9:15.
    private val underReview = AttendanceRecord(
        id = "a-1",
        employeeId = "ana",
        clockIn = phoneMark(at = bogotaInstant("2026-11-20 09:15"), deviceTime = bogotaInstant("2026-11-20 07:02")),
        clockOut = phoneMark(at = bogotaInstant("2026-11-20 14:01")),
    )
    private val forgotten = bogotaRecord("a-2", "luis", "2026-11-19 06:00")

    private val auth = FakeAuthRepository(userId = "admin")
    private val attendance = FakeAttendanceRepository(underReview, forgotten)
    private val corrections = FakeAttendanceCorrectionRepository(attendance)
    private val correctMark = CorrectMarkUseCase(auth, corrections, clock)
    private val registerMissingMarks = RegisterMissingMarksUseCase(auth, corrections, clock)

    @Test
    fun withOneTapTheTimeOfTheServerCountsWithoutAReason() = runTest {
        val corrected = correctMark(underReview, MarkKind.ENTRY, MarkTimeChoice.ServerTime).getOrThrow()

        val correction = corrections.corrections.single()
        assertEquals(CorrectionKind.SERVER_TIME, correction.kind)
        assertEquals(bogota("2026-11-20 09:15"), correction.time)
        assertNull(correction.reason)
        assertEquals("admin", correction.by)
        assertEquals(bogota("2026-11-20 09:15"), corrected.entryTime)
        assertEquals(underReview.clockIn, corrected.clockIn)
        assertEquals(now, corrected.corrections.single().at)
    }

    @Test
    fun withOneTapTheTimeOfTheDeviceCounts() = runTest {
        val corrected = correctMark(underReview, MarkKind.ENTRY, MarkTimeChoice.DeviceTime).getOrThrow()

        assertEquals(CorrectionKind.DEVICE_TIME, corrections.corrections.single().kind)
        assertEquals(bogota("2026-11-20 07:02"), corrected.entryTime)
    }

    @Test
    fun anotherTimeNeedsAReason() = runTest {
        val blank = correctMark(underReview, MarkKind.ENTRY, MarkTimeChoice.OtherTime(bogota("2026-11-20 07:00"), "  "))
        val corrected = correctMark(underReview, MarkKind.ENTRY, MarkTimeChoice.OtherTime(bogota("2026-11-20 07:00"), "  Llegó con el turno  ")).getOrThrow()

        assertEquals(CorrectionError.MissingReason, blank.exceptionOrNull())
        assertEquals("Llegó con el turno", corrections.corrections.single().reason)
        assertEquals(bogota("2026-11-20 07:00"), corrected.entryTime)
    }

    @Test
    fun anotherTimeForTheExitOfAForgottenEntryClosesIt() = runTest {
        val corrected = correctMark(forgotten, MarkKind.EXIT, MarkTimeChoice.OtherTime(bogota("2026-11-19 14:00"), "Se le olvidó marcar")).getOrThrow()

        val exit = attendance.records.value.single { it.id == "a-2" }.clockOut!!
        assertEquals(MarkSource.ADMIN, exit.source)
        assertEquals(bogota("2026-11-19 14:00"), exit.at)
        assertEquals(exit.at, exit.deviceTime)
        assertEquals(8.hours, corrected.workedTime)
        assertNull(attendance.observeOpenEntry("luis").first())
    }

    @Test
    fun aMarkWithoutThatTimeHasNothingToChoose() = runTest {
        val pending = underReview.copy(clockIn = underReview.clockIn.copy(at = null))

        assertEquals(CorrectionError.NothingToChoose, correctMark(pending, MarkKind.ENTRY, MarkTimeChoice.ServerTime).exceptionOrNull())
        assertEquals(CorrectionError.NothingToChoose, correctMark(forgotten, MarkKind.EXIT, MarkTimeChoice.DeviceTime).exceptionOrNull())
        assertTrue(corrections.corrections.isEmpty())
    }

    @Test
    fun theCorrectedTimesStillMakeAShift() = runTest {
        val afterTheExit = correctMark(underReview, MarkKind.ENTRY, MarkTimeChoice.OtherTime(bogota("2026-11-20 15:00"), "Error"))
        val tooLong = correctMark(forgotten, MarkKind.EXIT, MarkTimeChoice.OtherTime(bogota("2026-11-20 07:00"), "Error"))
        val inTheFuture = correctMark(forgotten, MarkKind.EXIT, MarkTimeChoice.OtherTime(now + 1.hours, "Error"))

        assertEquals(CorrectionError.ExitBeforeEntry, afterTheExit.exceptionOrNull())
        assertEquals(CorrectionError.TooLong, tooLong.exceptionOrNull())
        assertEquals(CorrectionError.InFuture, inTheFuture.exceptionOrNull())
        assertTrue(corrections.corrections.isEmpty())
    }

    @Test
    fun aCorrectionNeedsASessionAndTellsWhatTheServerSaid() = runTest {
        val withoutSession = CorrectMarkUseCase(FakeAuthRepository(userId = null), corrections, clock)
        corrections.failure = CorrectionError.Network

        assertEquals(CorrectionError.NoSession, withoutSession(underReview, MarkKind.ENTRY, MarkTimeChoice.ServerTime).exceptionOrNull())
        assertEquals(CorrectionError.Network, correctMark(underReview, MarkKind.ENTRY, MarkTimeChoice.ServerTime).exceptionOrNull())
    }

    @Test
    fun aShiftWithoutMarksGetsThemFromTheAdministratorWithAReason() = runTest {
        val record = registerMissingMarks("ana", bogota("2026-11-20 06:00"), bogota("2026-11-20 14:00"), "Marcó en papel").getOrThrow()

        assertEquals(listOf(MarkSource.ADMIN, MarkSource.ADMIN), listOf(record.clockIn.source, record.clockOut?.source))
        assertEquals(CorrectionKind.REGISTRATION, record.corrections.single().kind)
        assertEquals("Marcó en papel", record.corrections.single().reason)
        assertEquals(8.hours, record.workedTime)
        assertTrue(attendance.records.value.any { it.id == record.id })
    }

    @Test
    fun aRegistrationNeedsAReasonAndTimesOfAShiftThatEnded() = runTest {
        assertEquals(CorrectionError.MissingReason, registerMissingMarks("ana", bogota("2026-11-20 06:00"), bogota("2026-11-20 14:00"), "").exceptionOrNull())
        assertEquals(CorrectionError.InFuture, registerMissingMarks("ana", bogota("2026-11-21 06:00"), bogota("2026-11-21 14:00"), "Papel").exceptionOrNull())
        assertEquals(CorrectionError.ExitBeforeEntry, registerMissingMarks("ana", bogota("2026-11-20 14:00"), bogota("2026-11-20 06:00"), "Papel").exceptionOrNull())
    }

    @Test
    fun theReportReadsThePeriodWithAMarginOnEachSide() = runTest {
        val shifts = FakeShiftRepository(shift("s-1", "ana", "2026-11-20", "06:00", "14:00"), shift("s-2", "ana", "2026-12-05", "06:00", "14:00"))
        val team = FakeTeamAttendanceRepository(attendance)
        val settings = FakeSettingsRepository(HotelSettings(lateToleranceMinutes = 5, maxOpenEntryHours = 12))
        val period = AttendancePeriod(CalendarDate(2026, 11, 16), CalendarDate(2026, 11, 30))

        val data = LoadAttendanceReportUseCase(shifts, team, settings)(period).getOrThrow()

        assertEquals(listOf("s-1"), data.shifts.map { it.id })
        assertEquals(setOf("a-1", "a-2"), data.records.map { it.id }.toSet())
        assertEquals(period.startsAt - 2.days to period.endsAt + 2.days, team.reads.single())
        assertEquals(5 to 12, data.lateToleranceMinutes to data.maxOpenEntryHours)
    }

    @Test
    fun theReportSaysWhyItCouldNotBeRead() = runTest {
        val shifts = FakeShiftRepository()
        val team = FakeTeamAttendanceRepository(attendance)
        val load = LoadAttendanceReportUseCase(shifts, team, FakeSettingsRepository())
        val period = AttendancePeriod(CalendarDate(2026, 11, 16), CalendarDate(2026, 11, 30))

        shifts.readFailure = ScheduleError.Network
        assertEquals(ReportError.Network, load(period).exceptionOrNull())
        shifts.readFailure = null
        team.readFailure = ReportError.NotAllowed
        assertEquals(ReportError.NotAllowed, load(period).exceptionOrNull())
        assertEquals(ReportError.PeriodTooLong, load(AttendancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 12, 2))).exceptionOrNull())
    }

    @Test
    fun receptionAndAdministratorsSeeWhoIsWorkingByName() = runTest {
        val users = FakeUserRepository(
            employee("recepcion", "Rosa Díaz", UserType.RECEPTION),
            employee("ana", "Ana Rojas"),
            employee("luis", "Luis Pérez"),
        )
        val team = FakeTeamAttendanceRepository(attendance)
        attendance.records.value += bogotaRecord("a-3", "ana", "2026-11-21 06:00")

        val entries = ObserveWhoIsWorkingUseCase(FakeAuthRepository(userId = "recepcion"), users, team, FakeSettingsRepository())().first()!!

        assertEquals(listOf("Ana Rojas", "Luis Pérez"), entries.map { it.employeeName })
        assertEquals(bogota("2026-11-21 06:00") + 16.hours, entries.first().entry.forgottenAt)
    }

    @Test
    fun aBasicEmployeeDoesNotSeeWhoIsWorking() = runTest {
        val users = FakeUserRepository(employee("ana", "Ana Rojas"))

        assertNull(ObserveWhoIsWorkingUseCase(FakeAuthRepository(userId = "ana"), users, FakeTeamAttendanceRepository(attendance), FakeSettingsRepository())().first())
        assertNull(ObserveWhoIsWorkingUseCase(FakeAuthRepository(userId = null), users, FakeTeamAttendanceRepository(attendance), FakeSettingsRepository())().first())
    }
}
