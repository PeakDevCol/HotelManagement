package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceData
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceLine
import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.RecordStatus
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.pendingExits
import com.peakdevcol.project.hotelmanagement.domain.model.report
import com.peakdevcol.project.hotelmanagement.domain.model.shiftAmong
import com.peakdevcol.project.hotelmanagement.fakes.bogotaInstant
import com.peakdevcol.project.hotelmanagement.fakes.bogotaRecord
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import com.peakdevcol.project.hotelmanagement.fakes.shift
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** RF-ASI-06 and RF-ASI-08, with decisions 5A and 6A. */
class AttendanceReportTest {
    private fun bogota(text: String) = Instant.parse(bogotaInstant(text))

    private val fortnight = AttendancePeriod(CalendarDate(2026, 11, 16), CalendarDate(2026, 11, 30))
    private val now = bogota("2026-11-24 12:00")

    private val anaFriday = shift("ana-fri", "ana", "2026-11-20", "06:00", "14:00")
    private val anaSaturday = shift("ana-sat", "ana", "2026-11-21", "06:00", "14:00")
    private val anaLater = shift("ana-later", "ana", "2026-11-26", "06:00", "14:00")
    private val luisFriday = shift("luis-fri", "luis", "2026-11-20", "14:00", "22:00")

    private val anaOnTime = bogotaRecord("ana-1", "ana", "2026-11-20 06:05", "2026-11-20 14:01")
    private val luisLate = bogotaRecord("luis-1", "luis", "2026-11-20 14:25", "2026-11-20 22:00")
    private val anaWithoutShift = bogotaRecord("ana-2", "ana", "2026-11-22 15:00", "2026-11-22 17:00")
    private val luisForgotten = bogotaRecord("luis-2", "luis", "2026-11-23 14:00")

    private fun data(shifts: List<Shift>, records: List<AttendanceRecord>, period: AttendancePeriod = fortnight) =
        AttendanceData(period, shifts, records, lateToleranceMinutes = 10, maxOpenEntryHours = 16)

    private val report = data(
        shifts = listOf(anaFriday, anaSaturday, anaLater, luisFriday),
        records = listOf(anaOnTime, luisLate, anaWithoutShift, luisForgotten),
    ).report(now)

    private fun hoursOf(employeeId: String) = report.employees.single { it.employeeId == employeeId }

    @Test
    fun theHoursOfEachEmployeeComeFromTheirShiftsAndTheirMarks() {
        val ana = hoursOf("ana")

        assertEquals(24.hours, ana.scheduledTime)
        assertEquals(7.hours + 56.minutes + 2.hours, ana.workedTime)
        assertEquals(2.hours, ana.unscheduledTime)
        assertEquals(0, ana.lateCount)
        assertEquals(1, ana.missingCount)
    }

    @Test
    fun anEntryAfterTheStartPlusTheToleranceIsLate() {
        val luis = hoursOf("luis")
        val friday = luis.lines.filterIsInstance<AttendanceLine.Scheduled>().single()

        assertEquals(1, luis.lateCount)
        assertEquals(25.minutes, friday.lateBy)
    }

    @Test
    fun anEntryAtTheEndOfTheToleranceIsOnTime() {
        val atTheLimit = bogotaRecord("ana-3", "ana", "2026-11-20 06:10", "2026-11-20 14:00")
        val line = data(listOf(anaFriday), listOf(atTheLimit)).report(now).employees.single().lines.single()

        assertNull(assertIs<AttendanceLine.Scheduled>(line).lateBy)
    }

    @Test
    fun aShiftIsMissingOnlyOnceItEndedWithoutMarks() {
        val lines = hoursOf("ana").lines.filterIsInstance<AttendanceLine.Scheduled>().associateBy { it.shift.id }

        assertTrue(lines.getValue("ana-sat").isMissing)
        assertEquals(false, lines.getValue("ana-later").isMissing)
        assertEquals(false, lines.getValue("ana-fri").isMissing)
    }

    @Test
    fun aPendingExitAddsNoHoursAndIsCounted() {
        val luis = hoursOf("luis")
        val forgotten = luis.lines.filterIsInstance<AttendanceLine.Unscheduled>().single().record

        assertEquals(RecordStatus.PENDING_EXIT, forgotten.status)
        assertEquals(Duration.ZERO, forgotten.workedTime)
        assertEquals(1, luis.pendingExitCount)
        assertEquals(7.hours + 35.minutes, luis.workedTime)
    }

    @Test
    fun anOpenEntryWithinTheHoursOfTheParameterIsSomeoneWorking() {
        val working = bogotaRecord("ana-4", "ana", "2026-11-24 06:02")
        val line = data(listOf(shift("ana-tue", "ana", "2026-11-24", "06:00", "14:00")), listOf(working)).report(now).employees.single().lines.single()

        assertEquals(RecordStatus.WORKING, line.records.single().status)
    }

    @Test
    fun anEntryCountsForTheShiftThatStartsNearestWithinTwoHoursBeforeIt() {
        val morning = shift("morning", "ana", "2026-11-20", "06:00", "14:00")
        val afternoon = shift("afternoon", "ana", "2026-11-20", "14:00", "22:00")
        val shifts = listOf(morning, afternoon)

        assertEquals("morning", bogotaRecord("r", "ana", "2026-11-20 04:00").shiftAmong(shifts)?.id)
        assertNull(bogotaRecord("r", "ana", "2026-11-20 03:59").shiftAmong(listOf(morning)))
        assertEquals("afternoon", bogotaRecord("r", "ana", "2026-11-20 13:00").shiftAmong(shifts)?.id)
        assertNull(bogotaRecord("r", "luis", "2026-11-20 06:00").shiftAmong(shifts))
    }

    @Test
    fun twoEntriesInOneShiftAddUpAndTheFirstSaysIfItWasLate() {
        val first = bogotaRecord("r-1", "ana", "2026-11-20 06:20", "2026-11-20 10:00")
        val second = bogotaRecord("r-2", "ana", "2026-11-20 11:00", "2026-11-20 14:00")
        val line = data(listOf(anaFriday), listOf(second, first)).report(now).employees.single().lines.single() as AttendanceLine.Scheduled

        assertEquals(listOf("r-1", "r-2"), line.records.map { it.record.id })
        assertEquals(20.minutes, line.lateBy)
        assertEquals(3.hours + 40.minutes + 3.hours, line.records.fold(Duration.ZERO) { total, it -> total + it.workedTime })
    }

    @Test
    fun aLineBelongsToThePeriodByWhenItStarts() {
        // A night shift of the last day ends in December and still counts; an entry of the last
        // night for a shift of the first day after the period belongs to that shift, outside.
        val lastNight = shift("last-night", "ana", "2026-11-30", "22:00", "06:00")
        val nextDay = shift("next-day", "luis", "2026-12-01", "00:30", "08:30")
        val earlyForNextDay = bogotaRecord("r-1", "luis", "2026-11-30 23:30", "2026-12-01 08:30")
        val report = data(listOf(lastNight, nextDay), listOf(earlyForNextDay)).report(now)

        assertEquals(listOf("ana"), report.employees.map { it.employeeId })
        assertEquals(8.hours, report.employees.single().scheduledTime)
    }

    @Test
    fun aMarkToConfirmComesUpByItsTimeOrByWhenItReachedTheServer() {
        // Marked on the evening before the period, without connection; it reached the server inside it.
        val offlineBefore = AttendanceRecord(
            id = "r-1",
            employeeId = "ana",
            clockIn = phoneMark(at = bogotaInstant("2026-11-16 09:00"), deviceTime = bogotaInstant("2026-11-15 18:00")),
        )
        val confirmed = offlineBefore.copy(
            id = "r-2",
            corrections = listOf(Correction("c", CorrectionKind.DEVICE_TIME, MarkKind.ENTRY, bogota("2026-11-15 18:00"), null, "admin", bogota("2026-11-17 08:00"))),
        )
        val report = data(emptyList(), listOf(offlineBefore, confirmed, anaOnTime)).report(now)

        assertEquals(listOf("r-1"), report.toConfirm.map { it.id })
    }

    @Test
    fun theTotalsAddUpEveryEmployee() {
        assertEquals(32.hours, report.scheduledTime)
        assertEquals(7.hours + 56.minutes + 2.hours + 7.hours + 35.minutes, report.workedTime)
        assertEquals(1, report.lateCount)
        assertEquals(1, report.missingCount)
    }

    @Test
    fun thePendingExitsAreTheForgottenOpenEntriesTheOldestFirst() {
        val older = bogotaRecord("r-0", "ana", "2026-11-10 06:00")
        val open = listOf(luisForgotten, older, bogotaRecord("r-now", "ana", "2026-11-24 06:00"))

        assertEquals(listOf("r-0", "luis-2"), open.pendingExits(now, maxOpenEntryHours = 16).map { it.id })
    }
}
