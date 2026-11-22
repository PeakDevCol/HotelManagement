package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatDayRange
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_pick_period
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.stringResource

// The picker works with the first millisecond of each day in UTC, so a day is a whole number of these.
private const val MILLIS_PER_DAY = 86_400_000L

/** Chooses a first and a last day, like the period of the balance (RF-BAL-02) or of the report of hours (RF-ASI-08). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysRangeDialog(firstDay: CalendarDate, lastDay: CalendarDate, onConfirm: (firstEpochDay: Long, lastEpochDay: Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDaysRangePickerState(firstDay, lastDay)
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            MajesticButton(
                text = stringResource(Res.string.dialog_accept),
                onClick = { if (start != null && end != null) onConfirm(start / MILLIS_PER_DAY, end / MILLIS_PER_DAY) },
                style = ButtonStyle.Text,
                enabled = start != null && end != null,
            )
        },
        dismissButton = {
            MajesticButton(text = stringResource(Res.string.cancel), onClick = onDismiss, style = ButtonStyle.Text)
        },
    ) {
        DaysRangePicker(state = pickerState, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberDaysRangePickerState(firstDay: CalendarDate, lastDay: CalendarDate): DateRangePickerState =
    rememberDateRangePickerState(
        initialSelectedStartDateMillis = firstDay.epochDay * MILLIS_PER_DAY,
        initialSelectedEndDateMillis = lastDay.epochDay * MILLIS_PER_DAY,
    )

/** The calendar of [DaysRangeDialog], apart from the dialog so its layout can be checked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DaysRangePicker(state: DateRangePickerState, modifier: Modifier = Modifier) {
    DateRangePicker(
        state = state,
        title = {
            Text(
                text = stringResource(Res.string.balance_pick_period),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = MajesticSpacing.space6, end = MajesticSpacing.space3, top = MajesticSpacing.space4),
            )
        },
        // Material writes the range with its headline size, which does not fit the 360 dp of the
        // dialog and splits the last date in two, so the days are written the way the hotel reads them.
        headline = { ChosenDays(startMillis = state.selectedStartDateMillis, endMillis = state.selectedEndDateMillis) },
        modifier = modifier,
    )
}

@Composable
private fun ChosenDays(startMillis: Long?, endMillis: Long?) {
    if (startMillis == null) return
    val firstDay = CalendarDate.fromEpochDay(startMillis / MILLIS_PER_DAY)
    val text = if (endMillis == null) formatCalendarDate(firstDay) else formatDayRange(firstDay, CalendarDate.fromEpochDay(endMillis / MILLIS_PER_DAY))
    Text(
        text = text,
        style = MajesticTheme.typography.title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = MajesticSpacing.space6, end = MajesticSpacing.space3, bottom = MajesticSpacing.space3),
    )
}
