package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** RF-ASI-01: a shift assigned to an employee, stored at `Shifts/{id}`. It may cross midnight. */
data class Shift(
    val id: String,
    val employeeId: String,
    val startAt: Instant,
    val endAt: Instant,
    /** Where the employee works in this shift, when it is not their usual area. */
    val area: UserArea? = null,
)

/** Whether two shifts of the same employee share some time. One that ends when the other starts does not. */
fun Shift.overlaps(other: Shift): Boolean =
    id != other.id && employeeId == other.employeeId && startAt < other.endAt && other.startAt < endAt

/** RF-ASI-01: the shifts that cross another shift of the same employee, so the schedule warns about them. */
fun List<Shift>.overlappingIds(): Set<String> =
    groupBy { it.employeeId }.values.flatMap { shifts ->
        shifts.filter { shift -> shifts.any { other -> shift.overlaps(other) } }.map { it.id }
    }.toSet()

/**
 * A shift as the administrator writes it: a day and two times of a 24-hour clock. An end that is
 * not after the start means the next day, so a night shift crosses midnight (RF-ASI-01).
 */
data class ShiftDraft(
    val employeeId: String?,
    val day: CalendarDate,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val area: UserArea? = null,
) {
    /** The shift it describes, or why it does not describe one. */
    fun toShift(id: String): Result<Shift> {
        val employeeId = employeeId ?: return Result.failure(ScheduleError.MissingEmployee)
        val start = startMinutes?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return Result.failure(ScheduleError.InvalidTime)
        val end = endMinutes?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return Result.failure(ScheduleError.InvalidTime)
        if (start == end) return Result.failure(ScheduleError.EmptyShift)
        val startAt = startOfOperationalDay(day, dayStartMinutes = start)
        val endDay = if (end > start) day else day.plusDays(1)
        val endAt = startOfOperationalDay(endDay, dayStartMinutes = end)
        return Result.success(Shift(id = id, employeeId = employeeId, startAt = startAt, endAt = endAt, area = area))
    }

    companion object {
        /** The draft of an existing shift, to change it. */
        fun of(shift: Shift): ShiftDraft = ShiftDraft(
            employeeId = shift.employeeId,
            day = operationalDateOf(shift.startAt, dayStartMinutes = 0),
            startMinutes = minuteOfDayIn(shift.startAt),
            endMinutes = minuteOfDayIn(shift.endAt),
            area = shift.area,
        )
    }
}

/** The shifts that start on one day, in Colombian time. */
data class ShiftsOfDay(val date: CalendarDate, val shifts: List<Shift>)

/** RF-ASI-02: the shifts by the day they start, the earliest first. */
fun List<Shift>.byStartDay(): List<ShiftsOfDay> =
    sortedBy { it.startAt }
        .groupBy { operationalDateOf(it.startAt, dayStartMinutes = 0) }
        .map { (date, shifts) -> ShiftsOfDay(date, shifts) }

/** The first day of the week that holds [date]. Weeks go from Monday to Sunday, as in Colombia. */
fun weekStartOf(date: CalendarDate): CalendarDate {
    // 1970-01-01 was a Thursday, three days after a Monday.
    val daysSinceMonday = (date.epochDay + 3).mod(7L)
    return date.plusDays(-daysSinceMonday)
}

/** The seven days of the week that starts on [monday]. */
fun weekDays(monday: CalendarDate): List<CalendarDate> = (0L until 7L).map { monday.plusDays(it) }

/** The first moment of [date] in Colombian time. */
fun startOfBogotaDay(date: CalendarDate): Instant = startOfOperationalDay(date, dayStartMinutes = 0)

/** A shift lasts less than a day, so one that started the day before can still be going on. */
val SHIFT_LOOKBACK = 1.days

/** The same shift a number of days later, for copying a week. */
fun Shift.movedBy(days: Int, newId: String): Shift =
    copy(id = newId, startAt = startAt + (days * MINUTES_PER_DAY).minutes, endAt = endAt + (days * MINUTES_PER_DAY).minutes)

/** Why a change to the schedule was not made. */
sealed class ScheduleError(message: String) : Exception(message) {
    data object MissingEmployee : ScheduleError("The shift needs an employee")
    data object InvalidTime : ScheduleError("A time is not a valid time of the day")
    data object EmptyShift : ScheduleError("The shift starts and ends at the same time")
    data object NothingToCopy : ScheduleError("The week before has no shifts")
    data object NoSession : ScheduleError("There is no logged-in employee")
    data object NotAllowed : ScheduleError("Only an administrator changes the schedule")
    data object Network : ScheduleError("The server could not be reached")
    data object Unknown : ScheduleError("Unknown error")
}
