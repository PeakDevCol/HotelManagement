package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/** RF-ASI-08: the days of the report of hours, both included, as days of the Colombian calendar. */
data class AttendancePeriod(val firstDay: CalendarDate, val lastDay: CalendarDate) {

    val dayCount: Long get() = lastDay.epochDay - firstDay.epochDay + 1

    val startsAt: Instant get() = startOfBogotaDay(firstDay)

    /** The first moment that is no longer part of the period. */
    val endsAt: Instant get() = startOfBogotaDay(lastDay.plusDays(1))

    operator fun contains(instant: Instant): Boolean = instant >= startsAt && instant < endsAt

    companion object {
        /** RNF-12: a month at most, as for the balance, so one report does not use up the reads of the day. */
        const val MAX_DAYS = 31

        /** A period chosen day by day, or why it cannot be read. */
        fun between(firstDay: CalendarDate, lastDay: CalendarDate): Result<AttendancePeriod> {
            val period = AttendancePeriod(firstDay, lastDay)
            return when {
                lastDay < firstDay -> Result.failure(ReportError.InvalidPeriod)
                period.dayCount > MAX_DAYS -> Result.failure(ReportError.PeriodTooLong)
                else -> Result.success(period)
            }
        }
    }
}

/** The periods the report offers with one tap (decision 7A, 2026-11-21). */
enum class PeriodPreset {
    THIS_FORTNIGHT,
    LAST_FORTNIGHT,
    THIS_WEEK,
    THIS_MONTH,
    ;

    fun periodFor(today: CalendarDate): AttendancePeriod = when (this) {
        THIS_FORTNIGHT -> fortnightOf(today)
        LAST_FORTNIGHT -> fortnightOf(fortnightOf(today).firstDay.plusDays(-1))
        THIS_WEEK -> weekStartOf(today).let { monday -> AttendancePeriod(monday, monday.plusDays(6)) }
        THIS_MONTH -> AttendancePeriod(today.firstDayOfMonth, today.lastDayOfMonth)
    }
}

/** A fortnight as payroll counts it in Colombia: from the 1st to the 15th, or from the 16th to the end of the month. */
fun fortnightOf(date: CalendarDate): AttendancePeriod =
    if (date.day <= FIRST_FORTNIGHT_LAST_DAY) {
        AttendancePeriod(date.firstDayOfMonth, date.copy(day = FIRST_FORTNIGHT_LAST_DAY))
    } else {
        AttendancePeriod(date.copy(day = FIRST_FORTNIGHT_LAST_DAY + 1), date.lastDayOfMonth)
    }

private const val FIRST_FORTNIGHT_LAST_DAY = 15

/** Why the report of hours was not read. */
sealed class ReportError(message: String) : Exception(message) {
    data object InvalidPeriod : ReportError("The last day is before the first one")
    data object PeriodTooLong : ReportError("The period is longer than ${AttendancePeriod.MAX_DAYS} days")
    data object NotAllowed : ReportError("Only an administrator reads the report")
    data object Network : ReportError("The server could not be reached")
    data object Unknown : ReportError("Unknown error")
}
