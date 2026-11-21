package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_pick_period
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import org.jetbrains.compose.resources.stringResource

// The picker works with the first millisecond of each day in UTC, so a day is a whole number of these.
private const val MILLIS_PER_DAY = 86_400_000L

/** Chooses a first and a last day, like the period of the balance (RF-BAL-02) or of the report of hours (RF-ASI-08). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysRangeDialog(firstDay: CalendarDate, lastDay: CalendarDate, onConfirm: (firstEpochDay: Long, lastEpochDay: Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = firstDay.epochDay * MILLIS_PER_DAY,
        initialSelectedEndDateMillis = lastDay.epochDay * MILLIS_PER_DAY,
    )
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
        DateRangePicker(
            state = pickerState,
            title = { Text(stringResource(Res.string.balance_pick_period), modifier = Modifier.padding(start = MajesticSpacing.space6, top = MajesticSpacing.space4)) },
            modifier = Modifier.weight(1f),
        )
    }
}
