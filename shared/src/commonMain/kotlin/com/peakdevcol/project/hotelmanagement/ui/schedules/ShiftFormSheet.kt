package com.peakdevcol.project.hotelmanagement.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.weekDays
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.schedule_area
import com.peakdevcol.project.hotelmanagement.resources.schedule_area_usual
import com.peakdevcol.project.hotelmanagement.resources.schedule_day
import com.peakdevcol.project.hotelmanagement.resources.schedule_delete
import com.peakdevcol.project.hotelmanagement.resources.schedule_edit_shift
import com.peakdevcol.project.hotelmanagement.resources.schedule_employee
import com.peakdevcol.project.hotelmanagement.resources.schedule_end
import com.peakdevcol.project.hotelmanagement.resources.schedule_new_shift
import com.peakdevcol.project.hotelmanagement.resources.schedule_start
import com.peakdevcol.project.hotelmanagement.resources.schedule_times_hint
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.employees.label
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.stringResource

/**
 * RF-ASI-01: who, which day of the week shown, from when to when and, if it is not their usual
 * one, in which area. An end before the start ends the next day.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShiftFormSheet(form: ShiftForm, state: SchedulesState, onIntent: (SchedulesIntent) -> Unit) {
    val dismiss = { onIntent(SchedulesIntent.Dismiss) }
    TaskSheet(
        title = stringResource(if (form.shiftId == null) Res.string.schedule_new_shift else Res.string.schedule_edit_shift),
        onDismiss = dismiss,
        asDialog = LocalWideWindow.current,
    ) {
        ChoiceGroup(title = stringResource(Res.string.schedule_employee), error = form.error.takeIf { it == ScheduleError.MissingEmployee }) {
            // An inactive employee keeps the shift they already have, but gets no new ones.
            val employees = state.activeEmployees + state.employees.filter { it.id == form.employeeId && !it.active }
            employees.forEach { employee ->
                MajesticChip(text = employee.fullName, selected = form.employeeId == employee.id, onClick = { onIntent(SchedulesIntent.SelectEmployee(employee.id)) })
            }
        }
        ChoiceGroup(title = stringResource(Res.string.schedule_day)) {
            weekDays(state.monday).forEach { day ->
                MajesticChip(text = formatWeekday(day), selected = form.day == day, onClick = { onIntent(SchedulesIntent.SelectDay(day)) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
                TimeField(text = form.startText, label = stringResource(Res.string.schedule_start), hasError = form.hasTimeError, onChange = { onIntent(SchedulesIntent.StartChange(it)) }, modifier = Modifier.weight(1f))
                TimeField(text = form.endText, label = stringResource(Res.string.schedule_end), hasError = form.hasTimeError, onChange = { onIntent(SchedulesIntent.EndChange(it)) }, modifier = Modifier.weight(1f))
            }
            Text(
                text = stringResource(form.error.takeIf { form.hasTimeError }?.messageRes() ?: Res.string.schedule_times_hint),
                style = MajesticTheme.typography.caption,
                color = if (form.hasTimeError) MajesticTheme.colors.danger else MajesticTheme.colors.inkMuted,
            )
        }
        ChoiceGroup(title = stringResource(Res.string.schedule_area)) {
            MajesticChip(text = stringResource(Res.string.schedule_area_usual), selected = form.area == null, onClick = { onIntent(SchedulesIntent.SelectArea(null)) })
            UserArea.entries.forEach { area ->
                MajesticChip(text = stringResource(area.label()), selected = form.area == area, onClick = { onIntent(SchedulesIntent.SelectArea(area)) })
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
        ) {
            if (form.shiftId != null) {
                MajesticButton(text = stringResource(Res.string.schedule_delete), onClick = { onIntent(SchedulesIntent.DeleteShift) }, style = ButtonStyle.DangerOutline, enabled = !state.isWorking)
            }
            MajesticButton(text = stringResource(Res.string.cancel), onClick = dismiss, style = ButtonStyle.Text)
            MajesticButton(text = stringResource(Res.string.save), onClick = { onIntent(SchedulesIntent.SaveShift) }, enabled = !state.isWorking, loading = state.isWorking)
        }
    }
}

private val ShiftForm.hasTimeError: Boolean get() = error == ScheduleError.InvalidTime || error == ScheduleError.EmptyShift

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGroup(title: String, error: ScheduleError? = null, choices: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            choices()
        }
        if (error != null) {
            Text(text = stringResource(error.messageRes()), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.danger)
        }
    }
}

@Composable
private fun TimeField(text: String, label: String, hasError: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    MajesticTextField(
        value = text,
        onValueChange = onChange,
        label = label,
        placeholder = TIME_EXAMPLE,
        // The message goes once, under both fields; here only the red border.
        error = if (hasError) "" else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

private const val TIME_EXAMPLE = "06:00"
