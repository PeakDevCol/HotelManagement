package com.peakdevcol.project.hotelmanagement.ui.attendancereview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceLine
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeHours
import com.peakdevcol.project.hotelmanagement.domain.model.RecordStatus
import com.peakdevcol.project.hotelmanagement.domain.model.ReportedRecord
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.hours_employee_not_found
import com.peakdevcol.project.hotelmanagement.resources.hours_empty_title
import com.peakdevcol.project.hotelmanagement.resources.hours_hint
import com.peakdevcol.project.hotelmanagement.resources.hours_late
import com.peakdevcol.project.hotelmanagement.resources.hours_missing
import com.peakdevcol.project.hotelmanagement.resources.hours_scheduled
import com.peakdevcol.project.hotelmanagement.resources.hours_unscheduled
import com.peakdevcol.project.hotelmanagement.resources.hours_worked
import com.peakdevcol.project.hotelmanagement.resources.line_corrected
import com.peakdevcol.project.hotelmanagement.resources.line_late
import com.peakdevcol.project.hotelmanagement.resources.line_missing
import com.peakdevcol.project.hotelmanagement.resources.line_not_marked_yet
import com.peakdevcol.project.hotelmanagement.resources.line_record
import com.peakdevcol.project.hotelmanagement.resources.line_record_pending_exit
import com.peakdevcol.project.hotelmanagement.resources.line_record_working
import com.peakdevcol.project.hotelmanagement.resources.line_register
import com.peakdevcol.project.hotelmanagement.resources.line_shift
import com.peakdevcol.project.hotelmanagement.resources.line_unscheduled
import com.peakdevcol.project.hotelmanagement.resources.marking_needs_review
import com.peakdevcol.project.hotelmanagement.ui.attendance.durationLabel
import com.peakdevcol.project.hotelmanagement.ui.attendance.timeRange
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileData
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import org.jetbrains.compose.resources.stringResource

/**
 * RF-ASI-08: the shifts and the marks of one employee in the period, day by day. It shares the
 * view model of "Marcaciones", so it reads nothing again and a correction shows on both screens.
 */
@Composable
fun EmployeeAttendanceScreen(employeeId: String, viewModel: AttendanceReviewViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewEffects(viewModel)
    EmployeeAttendanceContent(state = state, employeeId = employeeId, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun EmployeeAttendanceContent(
    state: AttendanceReviewState,
    employeeId: String,
    onIntent: (AttendanceReviewIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { MajesticTopBar(title = employeeNameOrUnknown(state.nameOf(employeeId)), onBack = onBack) },
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
                val hours = state.hoursOf(employeeId)
                if (hours == null) {
                    EmptyState(icon = MajesticIcons.schedule, title = stringResource(Res.string.hours_empty_title), message = stringResource(Res.string.hours_employee_not_found))
                } else {
                    EmployeeTotals(hours)
                    Days(hours = hours, state = state, onIntent = onIntent)
                }
            }
        }
    }
    ReviewOverlays(state = state, onIntent = onIntent)
}

@Composable
private fun EmployeeTotals(hours: EmployeeHours) {
    Text(text = stringResource(Res.string.hours_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
    StatTileRow(
        tiles = listOfNotNull(
            StatTileData(label = stringResource(Res.string.hours_scheduled), value = durationLabel(hours.scheduledTime)),
            StatTileData(label = stringResource(Res.string.hours_worked), value = durationLabel(hours.workedTime)),
            StatTileData(label = stringResource(Res.string.hours_late), value = hours.lateCount.toString()),
            StatTileData(label = stringResource(Res.string.hours_missing), value = hours.missingCount.toString()),
            hours.unscheduledTime.takeIf { it.isPositive() }?.let { StatTileData(label = stringResource(Res.string.hours_unscheduled), value = durationLabel(it)) },
        ),
    )
}

/** The lines by the Colombian day they start on. */
@Composable
private fun Days(hours: EmployeeHours, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    hours.lines.groupBy { operationalDateOf(it.startsAt, dayStartMinutes = 0) }.forEach { (day, lines) ->
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            SectionLabel(text = formatWeekday(day))
            ListGroup {
                lines.forEachIndexed { index, line ->
                    if (index > 0) ListDivider()
                    LineRow(line = line, state = state, onIntent = onIntent)
                }
            }
        }
    }
}

/** A shift with its marks and how it went, or marks without a shift. Each mark opens its record. */
@Composable
private fun LineRow(line: AttendanceLine, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val colors = MajesticTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MajesticSpacing.space4, vertical = MajesticSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1),
    ) {
        Text(
            text = when (line) {
                is AttendanceLine.Scheduled -> stringResource(Res.string.line_shift, line.shift.timeRange())
                is AttendanceLine.Unscheduled -> stringResource(Res.string.line_unscheduled)
            },
            style = MajesticTheme.typography.bodyLarge,
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (line is AttendanceLine.Scheduled) ShiftOutcome(line = line, state = state, onIntent = onIntent)
        line.records.forEach { reported -> RecordLine(reported = reported, onClick = { onIntent(AttendanceReviewIntent.OpenRecord(reported.record.id)) }) }
    }
}

/** Late, without marks (with the registration of decision 4A), or not marked yet. */
@Composable
private fun ShiftOutcome(line: AttendanceLine.Scheduled, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val colors = MajesticTheme.colors
    val lateBy = line.lateBy
    when {
        lateBy != null -> Text(text = stringResource(Res.string.line_late, durationLabel(lateBy)), style = MajesticTheme.typography.body, color = colors.warning)
        line.isMissing -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            Text(text = stringResource(Res.string.line_missing), style = MajesticTheme.typography.body, color = colors.danger, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            MajesticButton(
                text = stringResource(Res.string.line_register),
                onClick = { onIntent(AttendanceReviewIntent.RegisterShift(line.shift.id)) },
                style = ButtonStyle.Text,
                icon = MajesticIcons.add,
            )
        }
        line.records.isEmpty() && line.shift.endAt > state.now ->
            Text(text = stringResource(Res.string.line_not_marked_yet), style = MajesticTheme.typography.body, color = colors.inkMuted)
    }
}

@Composable
private fun RecordLine(reported: ReportedRecord, onClick: () -> Unit) {
    val record = reported.record
    val entry = formatBogotaTime(record.entryTime)
    val exit = record.exitTime
    val marks = when {
        reported.status == RecordStatus.PENDING_EXIT -> stringResource(Res.string.line_record_pending_exit, entry)
        exit == null -> stringResource(Res.string.line_record_working, entry)
        else -> stringResource(Res.string.line_record, entry, formatBogotaTime(exit), durationLabel(reported.workedTime))
    }
    val notes = listOfNotNull(
        if (record.needsReview) stringResource(Res.string.marking_needs_review) else null,
        if (record.isCorrected) stringResource(Res.string.line_corrected) else null,
    )
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MajesticSizes.control).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = marks, style = MajesticTheme.typography.body, color = MajesticTheme.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (notes.isNotEmpty()) {
                Text(
                    text = notes.joinToString(" · "),
                    style = MajesticTheme.typography.caption,
                    color = if (record.needsReview || reported.status == RecordStatus.PENDING_EXIT) MajesticTheme.colors.warning else MajesticTheme.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted)
    }
}
