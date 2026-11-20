package com.peakdevcol.project.hotelmanagement.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduleDay
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduledShift
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.nav_schedules
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.resources.schedule_add_to_day
import com.peakdevcol.project.hotelmanagement.resources.schedule_copied
import com.peakdevcol.project.hotelmanagement.resources.schedule_copied_none
import com.peakdevcol.project.hotelmanagement.resources.schedule_copy
import com.peakdevcol.project.hotelmanagement.resources.schedule_copy_confirm
import com.peakdevcol.project.hotelmanagement.resources.schedule_copy_message
import com.peakdevcol.project.hotelmanagement.resources.schedule_copy_title
import com.peakdevcol.project.hotelmanagement.resources.schedule_day_empty
import com.peakdevcol.project.hotelmanagement.resources.schedule_delete_confirm
import com.peakdevcol.project.hotelmanagement.resources.schedule_delete_message
import com.peakdevcol.project.hotelmanagement.resources.schedule_delete_title
import com.peakdevcol.project.hotelmanagement.resources.schedule_deleted
import com.peakdevcol.project.hotelmanagement.resources.schedule_error
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_empty_shift
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_invalid_time
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_missing_employee
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_network
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_nothing_to_copy
import com.peakdevcol.project.hotelmanagement.resources.schedule_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.schedule_new_shift
import com.peakdevcol.project.hotelmanagement.resources.schedule_next_week
import com.peakdevcol.project.hotelmanagement.resources.schedule_overlap_line
import com.peakdevcol.project.hotelmanagement.resources.schedule_overlaps
import com.peakdevcol.project.hotelmanagement.resources.schedule_previous_week
import com.peakdevcol.project.hotelmanagement.resources.schedule_saved
import com.peakdevcol.project.hotelmanagement.resources.schedule_this_week
import com.peakdevcol.project.hotelmanagement.resources.schedule_week
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.shift_ends_next_day
import com.peakdevcol.project.hotelmanagement.ui.attendance.endsNextDay
import com.peakdevcol.project.hotelmanagement.ui.attendance.timeRange
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ConfirmDialog
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.employees.label
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-ASI-01: only administrators open it (PRD, section 3). */
@Composable
fun SchedulesScreen(viewModel: SchedulesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }

    SchedulesContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun SchedulesContent(state: SchedulesState, onIntent: (SchedulesIntent) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= MajesticSizes.mediumWidth
        Scaffold(
            topBar = {
                MajesticTopBar(title = stringResource(Res.string.nav_schedules)) {
                    if (wide) {
                        MajesticButton(
                            text = stringResource(Res.string.schedule_new_shift),
                            onClick = { onIntent(SchedulesIntent.NewShift()) },
                            icon = MajesticIcons.add,
                            modifier = Modifier.padding(end = MajesticSpacing.space3),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!wide) {
                    MajesticFloatingActionButton(
                        text = stringResource(Res.string.schedule_new_shift),
                        icon = MajesticIcons.add,
                        onClick = { onIntent(SchedulesIntent.NewShift()) },
                    )
                }
            },
            snackbarHost = { AppMessagesHost() },
            containerColor = MajesticTheme.colors.surface,
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MajesticSizes.content)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = MajesticSpacing.space4,
                            end = MajesticSpacing.space4,
                            top = MajesticSpacing.space2,
                            // Room for the floating button, so it never covers the last shift.
                            bottom = if (wide) MajesticSpacing.space4 else MajesticSizes.floatingActionButtonClearance,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
                ) {
                    WeekHeader(state = state, onIntent = onIntent)
                    Week(state = state, onIntent = onIntent)
                }
            }
        }
    }
    ShiftSheets(state = state, onIntent = onIntent)
}

/** The week shown, how to move to another one, and copying the one before. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekHeader(state: SchedulesState, onIntent: (SchedulesIntent) -> Unit) {
    val colors = MajesticTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MajesticIconButton(
                icon = MajesticIcons.chevronLeft,
                contentDescription = stringResource(Res.string.schedule_previous_week),
                onClick = { onIntent(SchedulesIntent.PreviousWeek) },
                tint = colors.ink,
            )
            Text(
                text = stringResource(Res.string.schedule_week, formatWeekday(state.monday), formatWeekday(state.monday.plusDays(6))),
                style = MajesticTheme.typography.title,
                color = colors.ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            MajesticIconButton(
                icon = MajesticIcons.chevronRight,
                contentDescription = stringResource(Res.string.schedule_next_week),
                onClick = { onIntent(SchedulesIntent.NextWeek) },
                tint = colors.ink,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
        ) {
            if (state.monday != state.thisMonday) {
                MajesticButton(text = stringResource(Res.string.schedule_this_week), onClick = { onIntent(SchedulesIntent.ThisWeek) }, style = ButtonStyle.Text)
            }
            MajesticButton(
                text = stringResource(Res.string.schedule_copy),
                onClick = { onIntent(SchedulesIntent.CopyPreviousWeek) },
                style = ButtonStyle.Outline,
                icon = MajesticIcons.copy,
                enabled = !state.isWorking,
            )
        }
        if (state.overlapCount > 0) {
            Banner(
                tone = BannerTone.Warning,
                icon = MajesticIcons.warning,
                message = pluralStringResource(Res.plurals.schedule_overlaps, state.overlapCount, state.overlapCount),
            )
        }
    }
}

@Composable
private fun Week(state: SchedulesState, onIntent: (SchedulesIntent) -> Unit) {
    when {
        state.isLoading -> MajesticLoading()
        state.loadFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.schedule_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(SchedulesIntent.Retry) },
        )
        else -> state.days.forEach { day -> Day(day = day, state = state, onIntent = onIntent) }
    }
}

@Composable
private fun Day(day: ScheduleDay, state: SchedulesState, onIntent: (SchedulesIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(text = formatWeekday(day.date), modifier = Modifier.weight(1f))
            MajesticButton(
                text = stringResource(Res.string.schedule_add_to_day),
                onClick = { onIntent(SchedulesIntent.NewShift(day.date)) },
                style = ButtonStyle.Text,
                icon = MajesticIcons.add,
            )
        }
        if (day.shifts.isEmpty()) {
            Text(text = stringResource(Res.string.schedule_day_empty), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
        } else {
            ListGroup {
                day.shifts.forEachIndexed { index, scheduled ->
                    if (index > 0) ListDivider()
                    ShiftRow(scheduled = scheduled, employeeName = employeeNameOrUnknown(state.nameOf(scheduled.shift.employeeId)), onIntent = onIntent)
                }
            }
        }
    }
}

@Composable
private fun ShiftRow(scheduled: ScheduledShift, employeeName: String, onIntent: (SchedulesIntent) -> Unit) {
    val shift = scheduled.shift
    val details = listOfNotNull(
        shift.timeRange(),
        if (shift.endsNextDay) stringResource(Res.string.shift_ends_next_day) else null,
        shift.area?.let { stringResource(it.label()) },
        if (scheduled.overlapsAnother) stringResource(Res.string.schedule_overlap_line) else null,
    )
    ListRow(
        title = employeeName,
        subtitle = details.joinToString(" · "),
        leading = { Avatar(initials = initialsOf(employeeName)) },
        trailing = if (scheduled.overlapsAnother) ({ MajesticIcon(icon = MajesticIcons.warning, contentDescription = null, tint = MajesticTheme.colors.warning) }) else null,
        onClick = { onIntent(SchedulesIntent.EditShift(shift)) },
    )
}

@Composable
private fun ShiftSheets(state: SchedulesState, onIntent: (SchedulesIntent) -> Unit) {
    val dismiss = { onIntent(SchedulesIntent.Dismiss) }
    state.form?.let { form -> ShiftFormSheet(form = form, state = state, onIntent = onIntent) }
    if (state.isConfirmingDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.schedule_delete_title),
            message = stringResource(Res.string.schedule_delete_message),
            confirmText = stringResource(Res.string.schedule_delete_confirm),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onIntent(SchedulesIntent.ConfirmDelete) },
            onDismiss = dismiss,
            destructive = true,
        )
    }
    if (state.isConfirmingCopy) {
        val previousMonday = state.monday.plusDays(-7)
        ConfirmDialog(
            title = stringResource(Res.string.schedule_copy_title),
            message = stringResource(Res.string.schedule_copy_message, formatWeekday(previousMonday), formatWeekday(previousMonday.plusDays(6))),
            confirmText = stringResource(Res.string.schedule_copy_confirm),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onIntent(SchedulesIntent.ConfirmCopy) },
            onDismiss = dismiss,
        )
    }
}

private suspend fun SchedulesEffect.message(): String = when (this) {
    SchedulesEffect.Saved -> getString(Res.string.schedule_saved)
    SchedulesEffect.Deleted -> getString(Res.string.schedule_deleted)
    is SchedulesEffect.Copied -> if (count == 0) getString(Res.string.schedule_copied_none) else getPluralString(Res.plurals.schedule_copied, count, count)
    is SchedulesEffect.Failed -> getString(error.messageRes())
}

fun ScheduleError.messageRes(): StringResource = when (this) {
    ScheduleError.MissingEmployee -> Res.string.schedule_error_missing_employee
    ScheduleError.InvalidTime -> Res.string.schedule_error_invalid_time
    ScheduleError.EmptyShift -> Res.string.schedule_error_empty_shift
    ScheduleError.NothingToCopy -> Res.string.schedule_error_nothing_to_copy
    ScheduleError.NoSession -> Res.string.order_error_no_session
    ScheduleError.NotAllowed -> Res.string.schedule_error_not_allowed
    ScheduleError.Network -> Res.string.schedule_error_network
    ScheduleError.Unknown -> Res.string.schedule_error_unknown
}
