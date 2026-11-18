package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/**
 * The operational days the balance covers, both included. Each one runs from the start of the
 * operational day to the start of the next (P17).
 */
data class BalancePeriod(val firstDay: CalendarDate, val lastDay: CalendarDate) {

    val dayCount: Long get() = lastDay.epochDay - firstDay.epochDay + 1

    fun startsAt(dayStartMinutes: Int): Instant = startOfOperationalDay(firstDay, dayStartMinutes)

    /** The first moment that is no longer part of the period. */
    fun endsAt(dayStartMinutes: Int): Instant = startOfOperationalDay(lastDay.plusDays(1), dayStartMinutes)

    companion object {
        /**
         * RNF-12: one read per payment, so a long period could use up a good part of the daily
         * quota of Spark. A month is what the hotel asked for (P6).
         */
        const val MAX_DAYS = 31

        /** RF-BAL-01: the month of the operational day of [now]. */
        fun monthOf(now: Instant, dayStartMinutes: Int): BalancePeriod {
            val today = operationalDateOf(now, dayStartMinutes)
            return BalancePeriod(today.firstDayOfMonth, today.lastDayOfMonth)
        }
    }
}

sealed class BalanceError(message: String) : Exception(message) {
    data object InvalidPeriod : BalanceError("The last day is before the first one")
    data object PeriodTooLong : BalanceError("The period is longer than ${BalancePeriod.MAX_DAYS} days")
}
