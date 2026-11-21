package com.peakdevcol.project.hotelmanagement.ui.attendancereview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeHours
import com.peakdevcol.project.hotelmanagement.domain.model.PeriodPreset
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatDayRange
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.hours_all_on_time
import com.peakdevcol.project.hotelmanagement.resources.hours_empty_message
import com.peakdevcol.project.hotelmanagement.resources.hours_empty_title
import com.peakdevcol.project.hotelmanagement.resources.hours_employees_title
import com.peakdevcol.project.hotelmanagement.resources.hours_hint
import com.peakdevcol.project.hotelmanagement.resources.hours_late
import com.peakdevcol.project.hotelmanagement.resources.hours_late_count
import com.peakdevcol.project.hotelmanagement.resources.hours_missing
import com.peakdevcol.project.hotelmanagement.resources.hours_missing_count
import com.peakdevcol.project.hotelmanagement.resources.hours_pending_exit_count
import com.peakdevcol.project.hotelmanagement.resources.hours_scheduled
import com.peakdevcol.project.hotelmanagement.resources.hours_to_confirm_count
import com.peakdevcol.project.hotelmanagement.resources.hours_unscheduled_time
import com.peakdevcol.project.hotelmanagement.resources.hours_worked
import com.peakdevcol.project.hotelmanagement.resources.hours_worked_of_scheduled
import com.peakdevcol.project.hotelmanagement.resources.nav_attendance_review
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.resources.review_both_to_confirm
import com.peakdevcol.project.hotelmanagement.resources.review_change_days
import com.peakdevcol.project.hotelmanagement.resources.review_corrected
import com.peakdevcol.project.hotelmanagement.resources.review_entry_of
import com.peakdevcol.project.hotelmanagement.resources.review_entry_to_confirm
import com.peakdevcol.project.hotelmanagement.resources.review_error_title
import com.peakdevcol.project.hotelmanagement.resources.review_exit_to_confirm
import com.peakdevcol.project.hotelmanagement.resources.review_nothing_message
import com.peakdevcol.project.hotelmanagement.resources.review_nothing_title
import com.peakdevcol.project.hotelmanagement.resources.review_pending_exits_hint
import com.peakdevcol.project.hotelmanagement.resources.review_pending_exits_title
import com.peakdevcol.project.hotelmanagement.resources.review_preset_custom
import com.peakdevcol.project.hotelmanagement.resources.review_read_at
import com.peakdevcol.project.hotelmanagement.resources.review_registered
import com.peakdevcol.project.hotelmanagement.resources.review_reload
import com.peakdevcol.project.hotelmanagement.resources.review_tab_hours
import com.peakdevcol.project.hotelmanagement.resources.review_tab_to_review
import com.peakdevcol.project.hotelmanagement.resources.review_tab_to_review_count
import com.peakdevcol.project.hotelmanagement.resources.review_to_confirm_hint
import com.peakdevcol.project.hotelmanagement.resources.review_to_confirm_title
import com.peakdevcol.project.hotelmanagement.resources.review_updating
import com.peakdevcol.project.hotelmanagement.ui.attendance.durationLabel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTabs
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ScrollingChipRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileData
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.DaysRangeDialog
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-ASI-05 to RF-ASI-08: only administrators open it (PRD, section 3 and P37). */
@Composable
fun AttendanceReviewScreen(
    onOpenEmployee: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AttendanceReviewViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewEffects(viewModel)
    AttendanceReviewContent(state = state, onIntent = viewModel::onIntent, onOpenEmployee = onOpenEmployee, onBack = onBack)
}

/** The messages of the corrections, shared with the detail of an employee, which uses the same view model. */
@Composable
fun ReviewEffects(viewModel: AttendanceReviewViewModel) {
    val messages = LocalAppMessages.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }
}

/** [onBack] is the arrow back to "Más", when the section was opened from there on the phone. */
@Composable
fun AttendanceReviewContent(
    state: AttendanceReviewState,
    onIntent: (AttendanceReviewIntent) -> Unit,
    onOpenEmployee: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            MajesticTopBar(title = stringResource(Res.string.nav_attendance_review), onBack = onBack) {
                MajesticIconButton(
                    icon = MajesticIcons.calendar,
                    contentDescription = stringResource(Res.string.review_change_days),
                    onClick = { onIntent(AttendanceReviewIntent.PickDays) },
                    tint = MajesticTheme.colors.ink,
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
                    .padding(MajesticSpacing.space4),
                verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
            ) {
                PeriodHeader(state = state, onIntent = onIntent)
                PeriodChoices(state = state, onIntent = onIntent)
                ReviewTabs(state = state, onIntent = onIntent)
                ReviewBody(state = state, onIntent = onIntent, onOpenEmployee = onOpenEmployee)
            }
        }
    }
    ReviewOverlays(state = state, onIntent = onIntent)
}

/** The days picker and the sheet of a record, on both screens of the section. */
@Composable
fun ReviewOverlays(state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    if (state.isPickingDays) {
        DaysRangeDialog(
            firstDay = state.period.firstDay,
            lastDay = state.period.lastDay,
            onConfirm = { first, last -> onIntent(AttendanceReviewIntent.DaysPicked(first, last)) },
            onDismiss = { onIntent(AttendanceReviewIntent.DismissDays) },
        )
    }
    state.editor?.let { editor -> RecordEditorSheet(editor = editor, state = state, onIntent = onIntent) }
}

/** The days shown, when they were read, and reading them again. */
@Composable
fun PeriodHeader(state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDayRange(state.period.firstDay, state.period.lastDay),
                style = MajesticTheme.typography.title,
                color = MajesticTheme.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val loadedAt = state.loadedAt
            Text(
                text = when {
                    state.isLoading -> stringResource(Res.string.review_updating)
                    loadedAt != null -> stringResource(Res.string.review_read_at, formatBogotaTime(loadedAt))
                    else -> ""
                },
                style = MajesticTheme.typography.caption,
                color = MajesticTheme.colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MajesticButton(
            text = stringResource(Res.string.review_reload),
            onClick = { onIntent(AttendanceReviewIntent.Reload) },
            style = ButtonStyle.Text,
            enabled = !state.isLoading,
        )
    }
}

/** Decision 7A: the fortnight first, and the other periods one tap away. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodChoices(state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val chips: @Composable () -> Unit = {
        PeriodPreset.entries.forEach { preset ->
            MajesticChip(text = stringResource(preset.label()), selected = state.preset == preset, onClick = { onIntent(AttendanceReviewIntent.SelectPreset(preset)) })
        }
        MajesticChip(
            text = stringResource(Res.string.review_preset_custom),
            selected = state.preset == null,
            onClick = { onIntent(AttendanceReviewIntent.PickDays) },
            icon = MajesticIcons.calendar,
        )
    }
    if (LocalWideWindow.current) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) { chips() }
    } else {
        ScrollingChipRow { chips() }
    }
}

@Composable
private fun ReviewTabs(state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val toReview = state.toReviewCount
    MajesticTabs(
        tabs = listOf(
            if (toReview > 0) stringResource(Res.string.review_tab_to_review_count, toReview) else stringResource(Res.string.review_tab_to_review),
            stringResource(Res.string.review_tab_hours),
        ),
        selectedIndex = state.tab.ordinal,
        onSelect = { index -> onIntent(AttendanceReviewIntent.SelectTab(ReviewTab.entries[index])) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReviewBody(state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit, onOpenEmployee: (String) -> Unit) {
    val loadError = state.loadError
    when {
        state.data == null && loadError != null -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.review_error_title),
            message = stringResource(loadError.messageRes()),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(AttendanceReviewIntent.Reload) },
        )
        state.data == null -> MajesticLoading()
        else -> {
            // A reading that failed after a good one keeps the report of before, with the error over it.
            if (loadError != null) {
                Banner(tone = BannerTone.Danger, message = stringResource(loadError.messageRes()), actionText = stringResource(Res.string.retry), onAction = { onIntent(AttendanceReviewIntent.Reload) })
            }
            when (state.tab) {
                ReviewTab.TO_REVIEW -> ToReview(state = state, onIntent = onIntent)
                ReviewTab.HOURS -> Hours(state = state, onOpenEmployee = onOpenEmployee)
            }
        }
    }
}

/** RF-ASI-06 and RF-ASI-05: the forgotten exits, from any day, and the marks of the period to confirm. */
@Composable
private fun ToReview(state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val toConfirm = state.report?.toConfirm.orEmpty()
    if (state.pendingExits.isEmpty() && toConfirm.isEmpty()) {
        EmptyState(icon = MajesticIcons.checkCircle, title = stringResource(Res.string.review_nothing_title), message = stringResource(Res.string.review_nothing_message))
        return
    }
    if (state.pendingExits.isNotEmpty()) {
        val hours = state.data?.maxOpenEntryHours ?: 0
        ReviewGroup(
            title = stringResource(Res.string.review_pending_exits_title),
            hint = pluralStringResource(Res.plurals.review_pending_exits_hint, hours, hours),
        ) {
            state.pendingExits.forEachIndexed { index, record ->
                if (index > 0) ListDivider()
                PendingExitRow(record = record, name = employeeNameOrUnknown(state.nameOf(record.employeeId)), onIntent = onIntent)
            }
        }
    }
    if (toConfirm.isNotEmpty()) {
        ReviewGroup(title = stringResource(Res.string.review_to_confirm_title), hint = stringResource(Res.string.review_to_confirm_hint)) {
            toConfirm.forEachIndexed { index, record ->
                if (index > 0) ListDivider()
                ToConfirmRow(record = record, name = employeeNameOrUnknown(state.nameOf(record.employeeId)), onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun ReviewGroup(title: String, hint: String, rows: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        Column {
            Text(text = title, style = MajesticTheme.typography.title, color = MajesticTheme.colors.ink)
            Text(text = hint, style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
        }
        ListGroup { rows() }
    }
}

/** RF-ASI-06: one tap goes straight to writing the exit. The group already says these are entries. */
@Composable
private fun PendingExitRow(record: AttendanceRecord, name: String, onIntent: (AttendanceReviewIntent) -> Unit) {
    ListRow(
        title = name,
        subtitle = "${formatWeekday(operationalDateOf(record.entryTime, dayStartMinutes = 0))}, ${formatBogotaTime(record.entryTime)}",
        leading = { Avatar(initials = initialsOf(name)) },
        trailing = { MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        onClick = { onIntent(AttendanceReviewIntent.SetMissingExit(record.id)) },
    )
}

/** "Entrada del vie 20 nov a las 6:02 a. m.". */
@Composable
fun AttendanceRecord.entryLabel(): String =
    stringResource(Res.string.review_entry_of, formatWeekday(operationalDateOf(entryTime, dayStartMinutes = 0)), formatBogotaTime(entryTime))

@Composable
private fun ToConfirmRow(record: AttendanceRecord, name: String, onIntent: (AttendanceReviewIntent) -> Unit) {
    val day = formatWeekday(operationalDateOf(record.entryTime, dayStartMinutes = 0))
    val subtitle = when {
        record.entryNeedsReview && record.exitNeedsReview -> stringResource(Res.string.review_both_to_confirm, day)
        record.entryNeedsReview -> stringResource(Res.string.review_entry_to_confirm, day)
        else -> stringResource(Res.string.review_exit_to_confirm, day)
    }
    ListRow(
        title = name,
        subtitle = subtitle,
        leading = { Avatar(initials = initialsOf(name)) },
        trailing = { MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        onClick = { onIntent(AttendanceReviewIntent.OpenRecord(record.id)) },
    )
}

/** RF-ASI-08: the totals of the team and the hours of each employee, by name. */
@Composable
private fun Hours(state: AttendanceReviewState, onOpenEmployee: (String) -> Unit) {
    val report = state.report ?: return
    Text(text = stringResource(Res.string.hours_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
    StatTileRow(
        tiles = listOf(
            StatTileData(label = stringResource(Res.string.hours_scheduled), value = durationLabel(report.scheduledTime)),
            StatTileData(label = stringResource(Res.string.hours_worked), value = durationLabel(report.workedTime)),
            StatTileData(label = stringResource(Res.string.hours_late), value = report.lateCount.toString()),
            StatTileData(label = stringResource(Res.string.hours_missing), value = report.missingCount.toString()),
        ),
    )
    val employees = state.employeeHours
    if (employees.isEmpty()) {
        EmptyState(icon = MajesticIcons.schedule, title = stringResource(Res.string.hours_empty_title), message = stringResource(Res.string.hours_empty_message))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        SectionLabel(text = stringResource(Res.string.hours_employees_title))
        ListGroup {
            employees.forEachIndexed { index, hours ->
                if (index > 0) ListDivider()
                EmployeeHoursRow(hours = hours, name = employeeNameOrUnknown(state.nameOf(hours.employeeId)), onClick = { onOpenEmployee(hours.employeeId) })
            }
        }
    }
}

/** The name, the hours worked of the scheduled ones, and what needs attention, on two lines at most. */
@Composable
private fun EmployeeHoursRow(hours: EmployeeHours, name: String, onClick: () -> Unit) {
    val colors = MajesticTheme.colors
    val issues = hours.issues()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MajesticSizes.control)
            .clickable(onClick = onClick)
            .padding(horizontal = MajesticSpacing.space4, vertical = MajesticSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        Avatar(initials = initialsOf(name))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MajesticTheme.typography.bodyLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(Res.string.hours_worked_of_scheduled, durationLabel(hours.workedTime), durationLabel(hours.scheduledTime)),
                style = MajesticTheme.typography.body,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = issues.ifEmpty { listOf(stringResource(Res.string.hours_all_on_time)) }.joinToString(" · "),
                style = MajesticTheme.typography.caption,
                color = if (issues.isEmpty()) colors.inkMuted else colors.warning,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = colors.inkMuted)
    }
}

/** Late arrivals, shifts without marks, pending exits, marks to confirm and hours without a shift. */
@Composable
fun EmployeeHours.issues(): List<String> = listOfNotNull(
    lateCount.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.hours_late_count, it, it) },
    missingCount.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.hours_missing_count, it, it) },
    pendingExitCount.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.hours_pending_exit_count, it, it) },
    toConfirmCount.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.hours_to_confirm_count, it, it) },
    unscheduledTime.takeIf { it.isPositive() }?.let { stringResource(Res.string.hours_unscheduled_time, durationLabel(it)) },
)

private suspend fun AttendanceReviewEffect.message(): String = when (this) {
    AttendanceReviewEffect.Corrected -> getString(Res.string.review_corrected)
    AttendanceReviewEffect.Registered -> getString(Res.string.review_registered)
    is AttendanceReviewEffect.PeriodRejected -> getString(error.messageRes())
}
