package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

// RNF-10: Colombia is always at UTC-5, without daylight saving time, so no time zone data is needed.
private const val BOGOTA_OFFSET_SECONDS = -5 * 3_600L
private const val SECONDS_PER_DAY = 86_400L
private const val SECONDS_PER_MINUTE = 60L
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * Formats a moment in Colombian time the way the hotel reads it: `14 nov 2026, 7:23 p. m.`.
 * It is written by hand because date formatting is not available in common code.
 */
fun formatBogotaDateTime(instant: Instant): String {
    val localSeconds = instant.epochSeconds + BOGOTA_OFFSET_SECONDS
    val date = CalendarDate.fromEpochDay(localSeconds.floorDiv(SECONDS_PER_DAY))
    val secondOfDay = localSeconds.mod(SECONDS_PER_DAY).toInt()
    return "${formatCalendarDate(date)}, ${formatTimeOfDay(minuteOfDay = secondOfDay / 60)}"
}

/** Formats a day the way the hotel reads it: `14 nov 2026`. */
fun formatCalendarDate(date: CalendarDate): String = "${date.day} ${MONTHS[date.month - 1]} ${date.year}"

/** Formats a time of the day given in minutes after midnight: `6:00 a. m.`, `11:30 p. m.`. */
fun formatTimeOfDay(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val hourOnClock = if (hour % 12 == 0) 12 else hour % 12
    val period = if (hour < 12) "a. m." else "p. m."
    return "$hourOnClock:${minute.toString().padStart(2, '0')} $period"
}

/**
 * The first moment of a Colombian calendar day written as `2026-11-01`, or null when the text is
 * not a real date.
 */
fun parseBogotaDate(text: String): Instant? =
    CalendarDate.parse(text)?.let { date -> startOfOperationalDay(date, dayStartMinutes = 0) }

/**
 * P17: the operational day a moment belongs to. Until [dayStartMinutes] after midnight, a sale
 * still belongs to the day before, so a night shift is not split in two.
 */
fun operationalDateOf(instant: Instant, dayStartMinutes: Int): CalendarDate {
    val secondsIntoOperationalDays = instant.epochSeconds + BOGOTA_OFFSET_SECONDS - dayStartMinutes * SECONDS_PER_MINUTE
    return CalendarDate.fromEpochDay(secondsIntoOperationalDays.floorDiv(SECONDS_PER_DAY))
}

/** The moment an operational day starts: [dayStartMinutes] after midnight, in Colombian time. */
fun startOfOperationalDay(date: CalendarDate, dayStartMinutes: Int): Instant =
    Instant.fromEpochSeconds(date.epochDay * SECONDS_PER_DAY - BOGOTA_OFFSET_SECONDS + dayStartMinutes * SECONDS_PER_MINUTE)
