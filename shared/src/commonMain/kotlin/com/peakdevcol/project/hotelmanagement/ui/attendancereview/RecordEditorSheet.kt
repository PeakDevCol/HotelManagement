package com.peakdevcol.project.hotelmanagement.ui.attendancereview

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
import androidx.compose.ui.text.style.TextOverflow
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.history
import com.peakdevcol.project.hotelmanagement.domain.model.isPendingExit
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.editor_change_entry
import com.peakdevcol.project.hotelmanagement.resources.editor_change_exit
import com.peakdevcol.project.hotelmanagement.resources.editor_form_entry_title
import com.peakdevcol.project.hotelmanagement.resources.editor_form_exit_title
import com.peakdevcol.project.hotelmanagement.resources.editor_form_registration_title
import com.peakdevcol.project.hotelmanagement.resources.editor_form_set_exit_title
import com.peakdevcol.project.hotelmanagement.resources.editor_history_title
import com.peakdevcol.project.hotelmanagement.resources.editor_mismatch_kiosk
import com.peakdevcol.project.hotelmanagement.resources.editor_mismatch_phone
import com.peakdevcol.project.hotelmanagement.resources.editor_mismatch_title
import com.peakdevcol.project.hotelmanagement.resources.editor_no_exit_pending
import com.peakdevcol.project.hotelmanagement.resources.editor_no_exit_working
import com.peakdevcol.project.hotelmanagement.resources.editor_other_time
import com.peakdevcol.project.hotelmanagement.resources.editor_reason
import com.peakdevcol.project.hotelmanagement.resources.editor_reason_hint
import com.peakdevcol.project.hotelmanagement.resources.editor_register
import com.peakdevcol.project.hotelmanagement.resources.editor_registration_hint
import com.peakdevcol.project.hotelmanagement.resources.editor_registration_preview
import com.peakdevcol.project.hotelmanagement.resources.editor_set_exit
import com.peakdevcol.project.hotelmanagement.resources.editor_subtitle_no_shift
import com.peakdevcol.project.hotelmanagement.resources.editor_subtitle_shift
import com.peakdevcol.project.hotelmanagement.resources.editor_time
import com.peakdevcol.project.hotelmanagement.resources.editor_time_hint
import com.peakdevcol.project.hotelmanagement.resources.editor_time_preview
import com.peakdevcol.project.hotelmanagement.resources.editor_title
import com.peakdevcol.project.hotelmanagement.resources.editor_use_kiosk_time
import com.peakdevcol.project.hotelmanagement.resources.editor_use_phone_time
import com.peakdevcol.project.hotelmanagement.resources.editor_use_server_time
import com.peakdevcol.project.hotelmanagement.resources.editor_worked
import com.peakdevcol.project.hotelmanagement.resources.history_by
import com.peakdevcol.project.hotelmanagement.resources.history_by_pending
import com.peakdevcol.project.hotelmanagement.resources.history_reason
import com.peakdevcol.project.hotelmanagement.resources.line_corrected
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.attendance.durationLabel
import com.peakdevcol.project.hotelmanagement.ui.attendance.timeRange
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** RF-ASI-07: a record, its history and how to correct it, or the registration of a shift without marks. */
@Composable
fun RecordEditorSheet(editor: RecordEditor, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    TaskSheet(
        title = stringResource(Res.string.editor_title, employeeNameOrUnknown(state.nameOf(editor.employeeId))),
        subtitle = editor.subtitle(),
        onDismiss = { onIntent(AttendanceReviewIntent.DismissEditor) },
        asDialog = LocalWideWindow.current,
    ) {
        RecordEditorFields(editor = editor, state = state, onIntent = onIntent)
    }
}

/** "jue 20 nov · turno 6:00 a. m. – 2:00 p. m.". */
@Composable
private fun RecordEditor.subtitle(): String {
    val start = shift?.startAt ?: record?.entryTime ?: return ""
    val day = formatWeekday(operationalDateOf(start, dayStartMinutes = 0))
    return shift?.let { stringResource(Res.string.editor_subtitle_shift, day, it.timeRange()) } ?: stringResource(Res.string.editor_subtitle_no_shift, day)
}

/** What the sheet shows, apart from its title, so the layout test can check it on its own. */
@Composable
fun RecordEditorFields(editor: RecordEditor, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    // An error about a field goes under that field; any other one, on top.
    editor.error?.takeIf { editor.form == null || (!it.isAboutTime && it != CorrectionError.MissingReason) }?.let { error ->
        Banner(tone = BannerTone.Danger, icon = MajesticIcons.error, message = stringResource(error.messageRes()))
    }
    val record = editor.record
    when (val form = editor.form) {
        null -> if (record != null) RecordOverview(record = record, state = state, onIntent = onIntent)
        is EditorForm.MarkTime -> MarkTimeForm(form = form, editor = editor, state = state, onIntent = onIntent)
        is EditorForm.Registration -> RegistrationForm(form = form, editor = editor, state = state, onIntent = onIntent)
    }
}

private val CorrectionError.isAboutTime: Boolean
    get() = this == CorrectionError.InvalidTime || this == CorrectionError.InFuture ||
        this == CorrectionError.ExitBeforeEntry || this == CorrectionError.TooLong

@Composable
private fun RecordOverview(record: AttendanceRecord, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    MarkSection(kind = MarkKind.ENTRY, record = record, state = state, onIntent = onIntent)
    MarkSection(kind = MarkKind.EXIT, record = record, state = state, onIntent = onIntent)
    record.workedTime?.let { worked ->
        Text(text = stringResource(Res.string.editor_worked, durationLabel(worked)), style = MajesticTheme.typography.bodyLarge, color = MajesticTheme.colors.ink)
    }
    if (record.isCorrected) History(record = record, state = state)
}

/**
 * One mark: the time that counts and where it was made. A mark whose clocks disagree offers both
 * times with one tap (decision 2A); any other time needs a reason.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkSection(kind: MarkKind, record: AttendanceRecord, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val mark = record.markOf(kind)
    val time = if (kind == MarkKind.ENTRY) record.entryTime else record.exitTime
    val needsReview = if (kind == MarkKind.ENTRY) record.entryNeedsReview else record.exitNeedsReview
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = stringResource(kind.title()))
        if (mark == null || time == null) {
            MissingExit(record = record, state = state, onIntent = onIntent)
            return@Column
        }
        Column {
            Text(text = formatBogotaDateTime(time), style = MajesticTheme.typography.bodyLarge, color = MajesticTheme.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val corrected = record.corrections.any { it.mark == kind }
            Text(
                text = listOfNotNull(mark.sourceLabel(), if (corrected) stringResource(Res.string.line_corrected) else null).joinToString(" · "),
                style = MajesticTheme.typography.caption,
                color = MajesticTheme.colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val serverTime = mark.at
        if (needsReview && serverTime != null) {
            val kiosk = mark.source == MarkSource.KIOSK
            Banner(
                tone = BannerTone.Warning,
                icon = MajesticIcons.warning,
                title = stringResource(Res.string.editor_mismatch_title),
                message = stringResource(
                    if (kiosk) Res.string.editor_mismatch_kiosk else Res.string.editor_mismatch_phone,
                    formatBogotaDateTime(mark.deviceTime),
                    formatBogotaDateTime(serverTime),
                ),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                MajesticButton(
                    text = stringResource(if (kiosk) Res.string.editor_use_kiosk_time else Res.string.editor_use_phone_time),
                    onClick = { onIntent(AttendanceReviewIntent.UseDeviceTime(kind)) },
                    style = ButtonStyle.Outline,
                    enabled = !state.isSaving,
                )
                MajesticButton(
                    text = stringResource(Res.string.editor_use_server_time),
                    onClick = { onIntent(AttendanceReviewIntent.UseServerTime(kind)) },
                    style = ButtonStyle.Outline,
                    enabled = !state.isSaving,
                )
                MajesticButton(text = stringResource(Res.string.editor_other_time), onClick = { onIntent(AttendanceReviewIntent.EditTime(kind)) }, style = ButtonStyle.Text, enabled = !state.isSaving)
            }
        } else {
            MajesticButton(
                text = stringResource(if (kind == MarkKind.ENTRY) Res.string.editor_change_entry else Res.string.editor_change_exit),
                onClick = { onIntent(AttendanceReviewIntent.EditTime(kind)) },
                style = ButtonStyle.Text,
                icon = MajesticIcons.edit,
                enabled = !state.isSaving,
            )
        }
    }
}

/** RF-ASI-06: only a forgotten entry gets its exit from the administrator; one in progress waits for the employee. */
@Composable
private fun MissingExit(record: AttendanceRecord, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val maxHours = state.data?.maxOpenEntryHours
    val pending = maxHours != null && record.isPendingExit(state.now, maxHours)
    Text(
        text = if (pending) pluralStringResource(Res.plurals.editor_no_exit_pending, maxHours, maxHours) else stringResource(Res.string.editor_no_exit_working),
        style = MajesticTheme.typography.body,
        color = if (pending) MajesticTheme.colors.warning else MajesticTheme.colors.inkMuted,
    )
    if (pending) {
        MajesticButton(text = stringResource(Res.string.editor_set_exit), onClick = { onIntent(AttendanceReviewIntent.EditTime(MarkKind.EXIT)) }, icon = MajesticIcons.logOut, enabled = !state.isSaving)
    }
}

/** RF-ASI-07: who changed what, when and why, the oldest first. */
@Composable
private fun History(record: AttendanceRecord, state: AttendanceReviewState) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
        FieldLabel(text = stringResource(Res.string.editor_history_title))
        record.history().forEach { step ->
            val correction = step.correction
            val by = employeeNameOrUnknown(state.nameOf(correction.by))
            Column {
                Text(text = step.description(record), style = MajesticTheme.typography.body, color = MajesticTheme.colors.ink)
                Text(
                    text = correction.at?.let { stringResource(Res.string.history_by, by, formatBogotaDateTime(it)) } ?: stringResource(Res.string.history_by_pending, by),
                    style = MajesticTheme.typography.caption,
                    color = MajesticTheme.colors.inkMuted,
                )
                correction.reason?.let { reason ->
                    Text(text = stringResource(Res.string.history_reason, reason), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun MarkTimeForm(form: EditorForm.MarkTime, editor: RecordEditor, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    val settingExit = form.mark == MarkKind.EXIT && editor.record?.clockOut == null
    Text(
        text = stringResource(
            when {
                settingExit -> Res.string.editor_form_set_exit_title
                form.mark == MarkKind.ENTRY -> Res.string.editor_form_entry_title
                else -> Res.string.editor_form_exit_title
            },
        ),
        style = MajesticTheme.typography.title,
        color = MajesticTheme.colors.ink,
    )
    // The exit is typed after the entry, so the entry stays in sight.
    if (form.mark == MarkKind.EXIT) {
        editor.record?.let { record -> Text(text = record.entryLabel(), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted) }
    }
    val timeError = editor.error?.takeIf { it.isAboutTime }
    val typed = editor.typedTime
    TimeField(text = form.timeText, label = stringResource(Res.string.editor_time), hasError = timeError != null, onChange = { onIntent(AttendanceReviewIntent.TimeChange(it)) }, modifier = Modifier.fillMaxWidth())
    FormNote(
        text = when {
            timeError != null -> stringResource(timeError.messageRes())
            typed != null -> stringResource(Res.string.editor_time_preview, formatBogotaDateTime(typed))
            else -> stringResource(Res.string.editor_time_hint)
        },
        isError = timeError != null,
    )
    ReasonField(form = form, error = editor.error, onIntent = onIntent)
    FormButtons(confirmText = stringResource(Res.string.save), state = state, onIntent = onIntent)
}

/** Decision 4A: the shift ended without marks; its times are suggested and the reason is required. */
@Composable
private fun RegistrationForm(form: EditorForm.Registration, editor: RecordEditor, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    Text(text = stringResource(Res.string.editor_form_registration_title), style = MajesticTheme.typography.title, color = MajesticTheme.colors.ink)
    Text(text = stringResource(Res.string.editor_registration_hint), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
    val timeError = editor.error?.takeIf { it.isAboutTime }
    Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
        TimeField(text = form.entryText, label = stringResource(MarkKind.ENTRY.title()), hasError = timeError != null, onChange = { onIntent(AttendanceReviewIntent.EntryChange(it)) }, modifier = Modifier.weight(1f))
        TimeField(text = form.exitText, label = stringResource(MarkKind.EXIT.title()), hasError = timeError != null, onChange = { onIntent(AttendanceReviewIntent.ExitChange(it)) }, modifier = Modifier.weight(1f))
    }
    val typed = editor.typedRegistration
    FormNote(
        text = when {
            timeError != null -> stringResource(timeError.messageRes())
            typed != null -> stringResource(Res.string.editor_registration_preview, formatBogotaDateTime(typed.first), formatBogotaDateTime(typed.second))
            else -> stringResource(Res.string.editor_time_hint)
        },
        isError = timeError != null,
    )
    ReasonField(form = form, error = editor.error, onIntent = onIntent)
    FormButtons(confirmText = stringResource(Res.string.editor_register), state = state, onIntent = onIntent)
}

@Composable
private fun TimeField(text: String, label: String, hasError: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    MajesticTextField(
        value = text,
        onValueChange = onChange,
        label = label,
        placeholder = TIME_EXAMPLE,
        // The message goes once, under the fields; here only the red border.
        error = if (hasError) "" else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@Composable
private fun FormNote(text: String, isError: Boolean) {
    Text(text = text, style = MajesticTheme.typography.caption, color = if (isError) MajesticTheme.colors.danger else MajesticTheme.colors.inkMuted)
}

@Composable
private fun ReasonField(form: EditorForm, error: CorrectionError?, onIntent: (AttendanceReviewIntent) -> Unit) {
    MajesticTextField(
        value = form.reasonText,
        onValueChange = { onIntent(AttendanceReviewIntent.ReasonChange(it)) },
        label = stringResource(Res.string.editor_reason),
        help = stringResource(Res.string.editor_reason_hint),
        error = if (error == CorrectionError.MissingReason) stringResource(error.messageRes()) else null,
        singleLine = false,
        minLines = 2,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormButtons(confirmText: String, state: AttendanceReviewState, onIntent: (AttendanceReviewIntent) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
    ) {
        MajesticButton(text = stringResource(Res.string.cancel), onClick = { onIntent(AttendanceReviewIntent.CancelForm) }, style = ButtonStyle.Text, enabled = !state.isSaving)
        MajesticButton(text = confirmText, onClick = { onIntent(AttendanceReviewIntent.Save) }, enabled = !state.isSaving, loading = state.isSaving)
    }
}

private const val TIME_EXAMPLE = "07:30"
