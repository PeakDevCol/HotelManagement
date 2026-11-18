package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_pick_period
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import org.jetbrains.compose.resources.stringResource

// The picker works with the first millisecond of each day in UTC, so a day is a whole number of these.
private const val MILLIS_PER_DAY = 86_400_000L

/** RF-BAL-02: chooses the first and the last operational day of the balance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancePeriodDialog(current: BalancePeriod, onConfirm: (firstEpochDay: Long, lastEpochDay: Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = current.firstDay.epochDay * MILLIS_PER_DAY,
        initialSelectedEndDateMillis = current.lastDay.epochDay * MILLIS_PER_DAY,
    )
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (start != null && end != null) onConfirm(start / MILLIS_PER_DAY, end / MILLIS_PER_DAY) },
                enabled = start != null && end != null,
            ) {
                Text(stringResource(Res.string.dialog_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            title = { Text(stringResource(Res.string.balance_pick_period), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            modifier = Modifier.weight(1f),
        )
    }
}
