package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** RF-ASI-07: what the administrator decided about a record. */
enum class CorrectionKind {
    /** The time of the phone or of the computer counts, for a mark whose two clocks disagree. */
    DEVICE_TIME,

    /** The time of the server counts. */
    SERVER_TIME,

    /** Another time, with a reason: a mark that was wrong, or the exit that was missing (RF-ASI-06). */
    OTHER_TIME,

    /** A shift without marks: the administrator wrote its entry and its exit, with a reason. */
    REGISTRATION,
}

/**
 * RF-ASI-07: one change of the administrator, kept with who made it, when and why. It never
 * overwrites a mark: the time that counts is the one of the last correction.
 */
data class Correction(
    val id: String,
    val kind: CorrectionKind,
    /** The mark it changes. Null for a registration, whose times are its marks. */
    val mark: MarkKind?,
    /** The time that counts from now on. Null for a registration. */
    val time: Instant?,
    val reason: String?,
    val by: String,
    /** The time of the server, null until the correction reaches it. */
    val at: Instant?,
)

/** How the administrator chooses the time of a mark (decision 2A, 2026-11-21). */
sealed interface MarkTimeChoice {
    /** One tap: the clock of the phone or of the computer was right. */
    data object DeviceTime : MarkTimeChoice

    /** One tap: the clock of the server was right. */
    data object ServerTime : MarkTimeChoice

    /** Any other time, always with a reason. */
    data class OtherTime(val time: Instant, val reason: String) : MarkTimeChoice
}

val MarkTimeChoice.kind: CorrectionKind
    get() = when (this) {
        MarkTimeChoice.DeviceTime -> CorrectionKind.DEVICE_TIME
        MarkTimeChoice.ServerTime -> CorrectionKind.SERVER_TIME
        is MarkTimeChoice.OtherTime -> CorrectionKind.OTHER_TIME
    }

const val MAX_REASON_LENGTH = 200

/** A shift lasts less than a day (RF-ASI-01), so a record longer than that has a wrong time. */
val MAX_RECORD_LENGTH: Duration = 24.hours

/** One line of the history of a record: a correction and the time that counted before it. */
data class CorrectionStep(val correction: Correction, val previousTime: Instant?)

/** RF-ASI-07: the corrections of the record, the oldest first, each with what it changed. */
fun AttendanceRecord.history(): List<CorrectionStep> {
    var entry: Instant? = clockIn.effectiveTime
    // An exit written by the administrator did not exist before their correction.
    var exit: Instant? = clockOut?.takeUnless { it.source == MarkSource.ADMIN }?.effectiveTime
    return corrections.map { correction ->
        when (correction.mark) {
            MarkKind.ENTRY -> CorrectionStep(correction, entry).also { entry = correction.time }
            MarkKind.EXIT -> CorrectionStep(correction, exit).also { exit = correction.time }
            null -> CorrectionStep(correction, previousTime = null).also {
                entry = clockIn.at
                exit = clockOut?.at
            }
        }
    }
}

/**
 * The moment of a time of the day typed for an entry: the one nearest to [reference], so a night
 * entry can move across midnight in both directions.
 */
fun entryNear(reference: Instant, minuteOfDay: Int): Instant {
    val day = operationalDateOf(reference, dayStartMinutes = 0)
    return (-1L..1L)
        .map { offset -> startOfOperationalDay(day.plusDays(offset), dayStartMinutes = minuteOfDay) }
        .minBy { (it - reference).absoluteValue }
}

/** The moment of a time of the day typed for an exit: the first one after [entry], as the end of a shift. */
fun exitAfter(entry: Instant, minuteOfDay: Int): Instant {
    val sameDay = startOfOperationalDay(operationalDateOf(entry, dayStartMinutes = 0), dayStartMinutes = minuteOfDay)
    return if (sameDay > entry) sameDay else sameDay + 1.days
}

/** Why the times of a record, as corrected, do not make a shift, or null when they do. */
fun AttendanceRecord.timesError(): CorrectionError? {
    val exit = exitTime ?: return null
    return when {
        exit <= entryTime -> CorrectionError.ExitBeforeEntry
        exit - entryTime > MAX_RECORD_LENGTH -> CorrectionError.TooLong
        else -> null
    }
}

/** Why a correction was not made. */
sealed class CorrectionError(message: String) : Exception(message) {
    data object InvalidTime : CorrectionError("The time is not a valid time of the day")
    data object MissingReason : CorrectionError("Another time needs a reason")
    data object InFuture : CorrectionError("The time is after now")
    data object ExitBeforeEntry : CorrectionError("The exit is not after the entry")
    data object TooLong : CorrectionError("More than a day between the entry and the exit")

    /** The mark has no time of the server, or no mark at all, to choose. */
    data object NothingToChoose : CorrectionError("The mark has no such time")

    /** Someone else changed the record meanwhile, like the employee marking the exit. */
    data object Changed : CorrectionError("The record changed meanwhile")
    data object NoSession : CorrectionError("There is no logged-in employee")
    data object NotAllowed : CorrectionError("Only an administrator corrects marks")
    data object Network : CorrectionError("The server could not be reached")
    data object Unknown : CorrectionError("Unknown error")
}
