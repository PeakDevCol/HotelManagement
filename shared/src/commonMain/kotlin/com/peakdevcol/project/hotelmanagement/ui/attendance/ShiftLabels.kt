package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.day_today
import com.peakdevcol.project.hotelmanagement.resources.day_tomorrow
import com.peakdevcol.project.hotelmanagement.resources.duration_minutes
import com.peakdevcol.project.hotelmanagement.resources.shift_hours
import com.peakdevcol.project.hotelmanagement.resources.shift_hours_and_minutes
import com.peakdevcol.project.hotelmanagement.resources.shift_time_range
import com.peakdevcol.project.hotelmanagement.resources.shift_ends_next_day
import com.peakdevcol.project.hotelmanagement.ui.employees.label
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/** "6:00 a. m. – 2:00 p. m.". */
@Composable
fun Shift.timeRange(): String = stringResource(Res.string.shift_time_range, formatBogotaTime(startAt), formatBogotaTime(endAt))

/** A night shift ends the day after it starts. */
val Shift.endsNextDay: Boolean
    get() = operationalDateOf(endAt, dayStartMinutes = 0) != operationalDateOf(startAt, dayStartMinutes = 0)

/** "8 h · termina al día siguiente · Recepción": what the time range does not say. */
@Composable
fun Shift.details(): String = listOfNotNull(
    lengthLabel(),
    if (endsNextDay) stringResource(Res.string.shift_ends_next_day) else null,
    area?.let { stringResource(it.label()) },
).joinToString(" · ")

/** How long the shift lasts: "8 h" or "7 h 30 min". */
@Composable
fun Shift.lengthLabel(): String = durationLabel(endAt - startAt)

/** "8 h", "7 h 30 min" or "45 min". */
@Composable
fun durationLabel(duration: Duration): String {
    val minutes = duration.inWholeMinutes.coerceAtLeast(0)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> stringResource(Res.string.duration_minutes, rest)
        rest == 0L -> stringResource(Res.string.shift_hours, hours)
        else -> stringResource(Res.string.shift_hours_and_minutes, hours, rest)
    }
}

/** "Hoy · jue 20 nov", "Mañana · vie 21 nov" or "sáb 22 nov". */
@Composable
fun dayLabel(date: CalendarDate, today: CalendarDate?): String = when (date) {
    today -> stringResource(Res.string.day_today, formatWeekday(date))
    today?.plusDays(1) -> stringResource(Res.string.day_tomorrow, formatWeekday(date))
    else -> formatWeekday(date)
}
