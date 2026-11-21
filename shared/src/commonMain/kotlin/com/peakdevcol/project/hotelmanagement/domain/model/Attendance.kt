package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Where a mark was made: the phone of the employee, or an authorized computer at reception (P29). */
enum class MarkSource {
    PHONE,
    KIOSK,

    /**
     * RF-ASI-07: written by the administrator, like the exit that was missing. Its time is the one
     * they gave, as the time of the server and of the device, so it is never under review.
     */
    ADMIN,
}

/**
 * An entry or an exit (RF-ASI-03 and RF-ASI-04). [at] is the time of the server, null until the mark
 * reaches it; [deviceTime] is the clock of the device that marked.
 */
data class Mark(
    val at: Instant?,
    val deviceTime: Instant,
    val source: MarkSource,
    /** The authorized computer, for a mark at reception. */
    val deviceId: String? = null,
    /** How far from the hotel, and how accurate, for a mark from a phone. Never the coordinates. */
    val distanceMeters: Int? = null,
    val accuracyMeters: Int? = null,
)

/** How far apart the two clocks of a mark can be before someone has to check it (PRD, section 13, 2026-11-20). */
val REVIEW_THRESHOLD: Duration = 5.minutes

/**
 * RF-ASI-05: a mark made without connection reaches the server later, and a phone can have its clock
 * wrong. In both cases the two times are far apart, and the mark waits for the administrator.
 */
val Mark.needsReview: Boolean
    get() = at != null && (at - deviceTime).absoluteValue > REVIEW_THRESHOLD

/** The time that counts: the device time while the mark is pending or under review (decision 4A). */
val Mark.effectiveTime: Instant
    get() = if (at == null || needsReview) deviceTime else at

/** A mark made by the administrator: the time they gave is the only one it has. */
fun adminMark(time: Instant): Mark = Mark(at = time, deviceTime = time, source = MarkSource.ADMIN)

/** One entry and, once marked, its exit. Stored at `Attendance/{id}`; never deleted. */
data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val clockIn: Mark,
    val clockOut: Mark? = null,
    /** RF-ASI-07: what the administrator changed, the oldest first. The marks keep their original values. */
    val corrections: List<Correction> = emptyList(),
) {
    val isOpen: Boolean get() = clockOut == null

    /** The time that counts for the entry: the last one the administrator gave it, or the one of the mark. */
    val entryTime: Instant get() = correctedTime(MarkKind.ENTRY) ?: clockIn.effectiveTime

    val exitTime: Instant? get() = correctedTime(MarkKind.EXIT) ?: clockOut?.effectiveTime

    /** RF-ASI-05: a mark whose two clocks disagree waits until the administrator says which time counts. */
    val entryNeedsReview: Boolean get() = clockIn.needsReview && correctedTime(MarkKind.ENTRY) == null

    val exitNeedsReview: Boolean get() = clockOut?.needsReview == true && correctedTime(MarkKind.EXIT) == null

    val needsReview: Boolean get() = entryNeedsReview || exitNeedsReview

    val isCorrected: Boolean get() = corrections.isNotEmpty()

    /** RF-ASI-04: the hours worked, once the exit is marked. */
    val workedTime: Duration? get() = exitTime?.let { it - entryTime }

    fun markOf(kind: MarkKind): Mark? = when (kind) {
        MarkKind.ENTRY -> clockIn
        MarkKind.EXIT -> clockOut
    }

    private fun correctedTime(kind: MarkKind): Instant? = corrections.lastOrNull { it.mark == kind }?.time
}

/**
 * RF-ASI-06: after the hours of the parameter an entry without exit is forgotten. It waits for the
 * administrator, and the next mark of the employee is a new entry instead of an exit hours later.
 */
fun AttendanceRecord.forgottenAt(maxOpenEntryHours: Int): Instant = entryTime + maxOpenEntryHours.hours

/** RF-ASI-06: an entry left without exit, which only the administrator closes. */
fun AttendanceRecord.isPendingExit(now: Instant, maxOpenEntryHours: Int): Boolean = isOpen && now >= forgottenAt(maxOpenEntryHours)

/** The entry without exit of an employee, and from when it counts as forgotten. */
data class OpenEntry(val record: AttendanceRecord, val forgottenAt: Instant) {
    fun isForgotten(now: Instant): Boolean = now >= forgottenAt
}

/** The entry that the next mark closes, or null when the next mark is an entry. */
fun AttendanceRecord?.toClose(now: Instant, maxOpenEntryHours: Int): AttendanceRecord? =
    this?.takeIf { it.isOpen && now < it.forgottenAt(maxOpenEntryHours) }

/** Whether a mark reached the server or waits on the device (RF-ASI-05). */
enum class MarkDelivery {
    SENT,
    PENDING,
}

enum class MarkKind {
    ENTRY,
    EXIT,
}

/** What happened when marking. [workedTime] comes with an exit. */
data class MarkOutcome(
    val kind: MarkKind,
    val delivery: MarkDelivery,
    val time: Instant,
    val workedTime: Duration? = null,
    /** At reception: who marked, since the computer is shared. */
    val employeeName: String? = null,
)

/** RF-ASI-11: a computer of reception allowed to mark attendance, stored at `Devices/{id}`. */
data class AuthorizedDevice(
    val id: String,
    val name: String,
    val authorizedBy: String,
    val authorizedAt: Instant? = null,
    val active: Boolean = true,
)

/** Why a mark, or a change to where marking is allowed, was not made. */
sealed class AttendanceError(message: String) : Exception(message) {
    data object NoSession : AttendanceError("There is no logged-in employee")
    data object NoZone : AttendanceError("The administrator has not set where the hotel is")
    data object LocationPermissionDenied : AttendanceError("The app may not read the location")
    data object LocationDisabled : AttendanceError("The location of the phone is off")
    data object LocationUnavailable : AttendanceError("This device cannot tell where it is")
    data object LocationTimedOut : AttendanceError("The phone did not find where it is in time")
    data object SimulatedLocation : AttendanceError("The location is faked by another app")
    data class OutsideZone(val distanceMeters: Int) : AttendanceError("The phone is outside the hotel")
    data class ImpreciseLocation(val accuracyMeters: Int) : AttendanceError("The location is not accurate enough")
    data object AlreadyOpen : AttendanceError("There is an entry without exit")
    data object NoOpenEntry : AttendanceError("There is no entry to close")
    data object InvalidCredentials : AttendanceError("The email or the password are wrong")
    data object InactiveAccount : AttendanceError("The account has no active employee profile")
    data object DeviceNotAuthorized : AttendanceError("This computer is not authorized to mark")
    data object InvalidName : AttendanceError("The computer needs a name")
    data object NotAllowed : AttendanceError("The rules did not allow it")
    data object Network : AttendanceError("The server could not be reached")
    data object Unknown : AttendanceError("Unknown error")
}
