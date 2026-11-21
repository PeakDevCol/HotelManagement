package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Decision 5A (2026-11-21): an entry counts for a shift from two hours before its start until its end. */
val EARLY_ENTRY_WINDOW: Duration = 2.hours

/** The shift an entry counts for: one of the same employee, the one that starts nearest to the entry. */
fun AttendanceRecord.shiftAmong(shifts: List<Shift>): Shift? =
    shifts
        .filter { it.employeeId == employeeId && entryTime >= it.startAt - EARLY_ENTRY_WINDOW && entryTime < it.endAt }
        .minByOrNull { (entryTime - it.startAt).absoluteValue }

/** How a record stands at the moment of the report. */
enum class RecordStatus {
    CLOSED,

    /** The entry is open within the hours of the parameter: the employee is working. */
    WORKING,

    /** RF-ASI-06: open for longer than the parameter. It adds no hours until the administrator closes it (6A). */
    PENDING_EXIT,
}

data class ReportedRecord(val record: AttendanceRecord, val status: RecordStatus) {
    /** Decision 6A: only a record with its exit adds hours. */
    val workedTime: Duration get() = record.workedTime?.takeIf { status == RecordStatus.CLOSED } ?: Duration.ZERO
}

/** One line of the detail of an employee, by when it starts. */
sealed interface AttendanceLine {
    val employeeId: String
    val startsAt: Instant
    val records: List<ReportedRecord>

    /** A shift with the records that count for it. [lateBy] is null when the employee arrived on time or did not mark. */
    data class Scheduled(
        val shift: Shift,
        override val records: List<ReportedRecord>,
        val lateBy: Duration?,
        /** The shift ended without any mark. One that has not ended yet is not missing. */
        val isMissing: Boolean,
    ) : AttendanceLine {
        override val employeeId: String get() = shift.employeeId
        override val startsAt: Instant get() = shift.startAt
    }

    /** Hours marked outside every shift. */
    data class Unscheduled(val record: ReportedRecord) : AttendanceLine {
        override val employeeId: String get() = record.record.employeeId
        override val startsAt: Instant get() = record.record.entryTime
        override val records: List<ReportedRecord> get() = listOf(record)
    }
}

/** RF-ASI-08: the hours of one employee in the period, and the lines they come from. */
data class EmployeeHours(val employeeId: String, val lines: List<AttendanceLine>) {
    val scheduledTime: Duration = lines.filterIsInstance<AttendanceLine.Scheduled>().sumOfDurations { it.shift.endAt - it.shift.startAt }
    val workedTime: Duration = lines.sumOfDurations { line -> line.records.sumOfDurations { it.workedTime } }
    val unscheduledTime: Duration = lines.filterIsInstance<AttendanceLine.Unscheduled>().sumOfDurations { it.record.workedTime }
    val lateCount: Int = lines.count { it is AttendanceLine.Scheduled && it.lateBy != null }
    val missingCount: Int = lines.count { it is AttendanceLine.Scheduled && it.isMissing }
    val pendingExitCount: Int = lines.sumOf { line -> line.records.count { it.status == RecordStatus.PENDING_EXIT } }
    val toConfirmCount: Int = lines.sumOf { line -> line.records.count { it.record.needsReview } }
}

/** What the report reads once from the server, with the parameters it is judged by. */
data class AttendanceData(
    val period: AttendancePeriod,
    val shifts: List<Shift>,
    val records: List<AttendanceRecord>,
    val lateToleranceMinutes: Int,
    val maxOpenEntryHours: Int,
) {
    /** After a correction the record changes here, so the report does not read the period again. */
    fun withRecord(record: AttendanceRecord): AttendanceData =
        copy(records = records.filterNot { it.id == record.id } + record)
}

data class AttendanceReport(
    /** Every employee with a shift or a mark in the period. */
    val employees: List<EmployeeHours>,
    /** RF-ASI-05: the records of the period with a mark whose two clocks disagree, the oldest first. */
    val toConfirm: List<AttendanceRecord>,
) {
    val scheduledTime: Duration = employees.sumOfDurations { it.scheduledTime }
    val workedTime: Duration = employees.sumOfDurations { it.workedTime }
    val lateCount: Int = employees.sumOf { it.lateCount }
    val missingCount: Int = employees.sumOf { it.missingCount }
}

/**
 * RF-ASI-08: the hours of the period at [now]. A line belongs to the period by when it starts: a
 * shift by its start, and a record without a shift by its entry. Late means after the start plus
 * the tolerance of the parameter.
 */
fun AttendanceData.report(now: Instant): AttendanceReport {
    val recordsByShift = records.groupBy { it.shiftAmong(shifts)?.id }
    val tolerance = lateToleranceMinutes.minutes
    val reported = { record: AttendanceRecord -> ReportedRecord(record, record.statusAt(now, maxOpenEntryHours)) }

    val scheduled = shifts.filter { it.startAt in period }.map { shift ->
        val shiftRecords = recordsByShift[shift.id].orEmpty().sortedBy { it.entryTime }
        val firstEntry = shiftRecords.firstOrNull()?.entryTime
        AttendanceLine.Scheduled(
            shift = shift,
            records = shiftRecords.map(reported),
            lateBy = firstEntry?.let { it - shift.startAt }?.takeIf { it > tolerance },
            isMissing = shiftRecords.isEmpty() && shift.endAt <= now,
        )
    }
    val unscheduled = recordsByShift[null].orEmpty().filter { it.entryTime in period }.map { AttendanceLine.Unscheduled(reported(it)) }

    return AttendanceReport(
        employees = (scheduled + unscheduled)
            .groupBy { it.employeeId }
            .map { (employeeId, lines) -> EmployeeHours(employeeId, lines.sortedBy { it.startsAt }) },
        // A record also counts by when it reached the server: one with the clock of its phone far
        // off would otherwise never come up for review.
        toConfirm = records
            .filter { it.needsReview && (it.entryTime in period || it.clockIn.at?.let { at -> at in period } == true) }
            .sortedBy { it.entryTime },
    )
}

/** RF-ASI-06: the entries left without exit, the oldest first. */
fun List<AttendanceRecord>.pendingExits(now: Instant, maxOpenEntryHours: Int): List<AttendanceRecord> =
    filter { it.isPendingExit(now, maxOpenEntryHours) }.sortedBy { it.entryTime }

private fun AttendanceRecord.statusAt(now: Instant, maxOpenEntryHours: Int): RecordStatus = when {
    !isOpen -> RecordStatus.CLOSED
    isPendingExit(now, maxOpenEntryHours) -> RecordStatus.PENDING_EXIT
    else -> RecordStatus.WORKING
}

private inline fun <T> Iterable<T>.sumOfDurations(selector: (T) -> Duration): Duration =
    fold(Duration.ZERO) { total, item -> total + selector(item) }
