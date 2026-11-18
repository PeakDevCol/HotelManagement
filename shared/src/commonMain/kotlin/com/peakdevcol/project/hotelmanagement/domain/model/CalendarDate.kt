package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * A day of the calendar, without time or zone. It is written by hand because dates are not
 * available in common code without a new dependency.
 */
data class CalendarDate(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDate> {

    /** Days since 1970-01-01. */
    val epochDay: Long get() = daysSinceEpoch(year, month, day)

    val firstDayOfMonth: CalendarDate get() = copy(day = 1)

    val lastDayOfMonth: CalendarDate
        get() {
            val firstOfNextMonth = if (month == 12) CalendarDate(year + 1, 1, 1) else CalendarDate(year, month + 1, 1)
            return firstOfNextMonth.plusDays(-1)
        }

    fun plusDays(days: Long): CalendarDate = fromEpochDay(epochDay + days)

    override fun compareTo(other: CalendarDate): Int = epochDay.compareTo(other.epochDay)

    companion object {
        /** Howard Hinnant's civil_from_days algorithm. */
        fun fromEpochDay(epochDay: Long): CalendarDate {
            val shifted = epochDay + 719_468
            val era = shifted.floorDiv(146_097)
            val dayOfEra = shifted - era * 146_097
            val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val shiftedMonth = (5 * dayOfYear + 2) / 153
            val day = dayOfYear - (153 * shiftedMonth + 2) / 5 + 1
            val month = if (shiftedMonth < 10) shiftedMonth + 3 else shiftedMonth - 9
            val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
            return CalendarDate(year = year.toInt(), month = month.toInt(), day = day.toInt())
        }

        /** Reads a date written as `2026-11-01`, or returns null when it is not a real date. */
        fun parse(text: String): CalendarDate? {
            val parts = text.trim().split('-')
            if (parts.size != 3 || parts.any { part -> part.isEmpty() || !part.all(Char::isDigit) }) return null
            val (year, month, day) = parts.map { it.toIntOrNull() ?: return null }
            val date = CalendarDate(year, month, day)
            return date.takeIf { fromEpochDay(it.epochDay) == it }
        }
    }
}

/** Howard Hinnant's days_from_civil algorithm. */
private fun daysSinceEpoch(year: Int, month: Int, day: Int): Long {
    val shiftedYear = (if (month <= 2) year - 1 else year).toLong()
    val era = shiftedYear.floorDiv(400)
    val yearOfEra = shiftedYear - era * 400
    val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097 + dayOfEra - 719_468
}
