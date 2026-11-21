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

/** Only the time of a moment, in Colombian time: `7:23 p. m.`. */
fun formatBogotaTime(instant: Instant): String {
    val localSeconds = instant.epochSeconds + BOGOTA_OFFSET_SECONDS
    return formatTimeOfDay(minuteOfDay = localSeconds.mod(SECONDS_PER_DAY).toInt() / 60)
}

/** Whole minutes from [from] to [to], never negative even if the clocks of two devices differ. */
fun minutesBetween(from: Instant, to: Instant): Long = (to - from).inWholeMinutes.coerceAtLeast(0)

private val WEEKDAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

/** The minutes after midnight of a moment, in Colombian time. */
fun minuteOfDayIn(instant: Instant): Int {
    val localSeconds = instant.epochSeconds + BOGOTA_OFFSET_SECONDS
    return localSeconds.mod(SECONDS_PER_DAY).toInt() / 60
}

/** A day with its weekday, the way a schedule reads it: `lun 16 nov`. */
fun formatWeekday(date: CalendarDate): String {
    // 1970-01-01 was a Thursday, three days after a Monday.
    val weekday = WEEKDAYS[(date.epochDay + 3).mod(7L).toInt()]
    return "$weekday ${date.day} ${MONTHS[date.month - 1]}"
}

/**
 * Reads a time of a 24-hour clock, in minutes after midnight: `6`, `18`, `6:30` and `18:30`, or
 * only digits, `630` and `1830`, since the numeric keyboard of a phone has no colon. Null when it
 * is not a time of the day.
 */
fun parseClockTime(text: String): Int? {
    val trimmed = text.trim()
    val parts = if (':' in trimmed) {
        trimmed.split(':')
    } else if (trimmed.length > 2) {
        listOf(trimmed.dropLast(2), trimmed.takeLast(2))
    } else {
        listOf(trimmed)
    }
    if (parts.size > 2 || parts.any { part -> part.isEmpty() || part.length > 2 || !part.all(Char::isDigit) }) return null
    if (parts.size == 2 && parts[1].length != 2) return null
    val hour = parts[0].toInt()
    val minute = parts.getOrNull(1)?.toInt() ?: 0
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

/** A time of a 24-hour clock written for a field: `06:30`. */
fun formatClockTime(minuteOfDay: Int): String =
    "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"

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
