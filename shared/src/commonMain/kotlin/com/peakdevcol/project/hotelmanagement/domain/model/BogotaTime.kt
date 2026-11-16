package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

// RNF-10: Colombia is always at UTC-5, without daylight saving time, so no time zone data is needed.
private const val BOGOTA_OFFSET_SECONDS = -5 * 3_600L
private const val SECONDS_PER_DAY = 86_400L
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * Formats a moment in Colombian time the way the hotel reads it: `14 nov 2026, 7:23 p. m.`.
 * It is written by hand because date formatting is not available in common code.
 */
fun formatBogotaDateTime(instant: Instant): String {
    val localSeconds = instant.epochSeconds + BOGOTA_OFFSET_SECONDS
    val date = civilDate(daysSinceEpoch = localSeconds.floorDiv(SECONDS_PER_DAY))
    val secondOfDay = localSeconds.mod(SECONDS_PER_DAY).toInt()
    return "${date.day} ${MONTHS[date.month - 1]} ${date.year}, ${formatTime(secondOfDay)}"
}

private fun formatTime(secondOfDay: Int): String {
    val hour = secondOfDay / 3_600
    val minute = secondOfDay % 3_600 / 60
    val hourOnClock = if (hour % 12 == 0) 12 else hour % 12
    val period = if (hour < 12) "a. m." else "p. m."
    return "$hourOnClock:${minute.toString().padStart(2, '0')} $period"
}

private data class CivilDate(val year: Int, val month: Int, val day: Int)

/** Days since 1970-01-01 to a calendar date (Howard Hinnant's civil_from_days algorithm). */
private fun civilDate(daysSinceEpoch: Long): CivilDate {
    val shifted = daysSinceEpoch + 719_468
    val era = shifted.floorDiv(146_097)
    val dayOfEra = shifted - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val shiftedMonth = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * shiftedMonth + 2) / 5 + 1
    val month = if (shiftedMonth < 10) shiftedMonth + 3 else shiftedMonth - 9
    val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
    return CivilDate(year = year.toInt(), month = month.toInt(), day = day.toInt())
}
