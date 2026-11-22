package com.peakdevcol.project.hotelmanagement.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatThousands
import com.peakdevcol.project.hotelmanagement.domain.model.formatTimeOfDay
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.devices_title
import com.peakdevcol.project.hotelmanagement.resources.employee_error_network
import com.peakdevcol.project.hotelmanagement.resources.employee_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_error
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_title
import com.peakdevcol.project.hotelmanagement.resources.menu_settings
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.settings_attendance_error
import com.peakdevcol.project.hotelmanagement.resources.settings_attendance_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_attendance_title
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_date
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_date_error
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_date_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_done
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_error_file
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_export
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_only_desktop
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_personal_data
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_title
import com.peakdevcol.project.hotelmanagement.resources.settings_day_start_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_day_start_preview
import com.peakdevcol.project.hotelmanagement.resources.settings_day_start_title
import com.peakdevcol.project.hotelmanagement.resources.settings_devices_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_error
import com.peakdevcol.project.hotelmanagement.resources.settings_hotel_zone_none
import com.peakdevcol.project.hotelmanagement.resources.settings_hotel_zone_set
import com.peakdevcol.project.hotelmanagement.resources.settings_hour
import com.peakdevcol.project.hotelmanagement.resources.settings_last_change
import com.peakdevcol.project.hotelmanagement.resources.settings_late_tolerance
import com.peakdevcol.project.hotelmanagement.resources.settings_marking_title
import com.peakdevcol.project.hotelmanagement.resources.settings_max_open_hours
import com.peakdevcol.project.hotelmanagement.resources.settings_minute
import com.peakdevcol.project.hotelmanagement.resources.settings_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.settings_saved
import com.peakdevcol.project.hotelmanagement.resources.settings_time_error
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-EMP-06, RF-EMP-07, RF-ASI-10 and RF-ASI-11: the parameters of the hotel and the manual backup. */
@Composable
fun SettingsScreen(
    onOpenHotelZone: () -> Unit,
    onOpenDevices: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToHotelZone -> onOpenHotelZone()
                SettingsEffect.NavigateToDevices -> onOpenDevices()
                else -> effect.message()?.let { messages.show(it) }
            }
        }
    }

    SettingsContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

/** [onBack] is the arrow back to "Más", when the section was opened from there on the phone. */
@Composable
fun SettingsContent(state: SettingsState, onIntent: (SettingsIntent) -> Unit, onBack: (() -> Unit)? = null) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.menu_settings), onBack = onBack) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.loadFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.settings_error),
                message = stringResource(Res.string.error_check_connection),
                modifier = Modifier.padding(padding),
            )
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MajesticSizes.content)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(MajesticSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
                ) {
                    OperationalDayStart(state = state, onIntent = onIntent)
                    AttendanceRules(state = state, onIntent = onIntent)
                    MarkingPlaces(state = state, onIntent = onIntent)
                    Backup(state = state, onIntent = onIntent)
                }
            }
        }
    }
}

/** RF-EMP-06: when a day ends, so the sales after midnight count for the day before. */
@Composable
private fun OperationalDayStart(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val detail = state.detail ?: return
    SectionCard(title = stringResource(Res.string.settings_day_start_title), help = stringResource(Res.string.settings_day_start_hint)) {
        Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
            TimeField(
                value = state.hourText,
                label = stringResource(Res.string.settings_hour),
                onChange = { onIntent(SettingsIntent.HourChange(it)) },
                hasError = state.showTimeError,
                modifier = Modifier.weight(1f),
            )
            TimeField(
                value = state.minuteText,
                label = stringResource(Res.string.settings_minute),
                onChange = { onIntent(SettingsIntent.MinuteChange(it)) },
                hasError = state.showTimeError,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.showTimeError) {
            Text(text = stringResource(Res.string.settings_time_error), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.danger)
        }
        Text(
            text = stringResource(Res.string.settings_day_start_preview, formatTimeOfDay(detail.settings.operationalDayStartMinutes)),
            style = MajesticTheme.typography.body,
            color = MajesticTheme.colors.ink,
        )
        detail.settings.updatedAt?.let { updatedAt ->
            Text(
                text = stringResource(Res.string.settings_last_change, employeeNameOrUnknown(detail.updatedByName), formatBogotaDateTime(updatedAt)),
                style = MajesticTheme.typography.caption,
                color = MajesticTheme.colors.inkMuted,
            )
        }
        MajesticButton(
            text = stringResource(Res.string.save),
            onClick = { onIntent(SettingsIntent.SaveOperationalDayStart) },
            enabled = !state.isWorking,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TimeField(value: String, label: String, onChange: (String) -> Unit, hasError: Boolean, modifier: Modifier) =
    NumberField(value = value, label = label, onChange = onChange, hasError = hasError, modifier = modifier)

@Composable
private fun NumberField(value: String, label: String, onChange: (String) -> Unit, hasError: Boolean, modifier: Modifier) {
    MajesticTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        // The message goes once, under both fields; here only the red border.
        error = if (hasError) "" else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** RF-ASI-06 and RF-ASI-08: when an entry counts as late, and when one without exit is forgotten. */
@Composable
private fun AttendanceRules(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    SectionCard(title = stringResource(Res.string.settings_attendance_title), help = stringResource(Res.string.settings_attendance_hint)) {
        Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
            NumberField(
                value = state.toleranceText,
                label = stringResource(Res.string.settings_late_tolerance),
                hasError = state.showAttendanceError,
                onChange = { onIntent(SettingsIntent.ToleranceChange(it)) },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = state.maxOpenHoursText,
                label = stringResource(Res.string.settings_max_open_hours),
                hasError = state.showAttendanceError,
                onChange = { onIntent(SettingsIntent.MaxOpenHoursChange(it)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.showAttendanceError) {
            Text(text = stringResource(Res.string.settings_attendance_error), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.danger)
        }
        MajesticButton(
            text = stringResource(Res.string.save),
            onClick = { onIntent(SettingsIntent.SaveAttendanceRules) },
            enabled = !state.isWorking,
            loading = state.isSavingRules,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** RF-ASI-10 and RF-ASI-11: where the attendance is marked, each in its own screen. */
@Composable
private fun MarkingPlaces(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val zone = state.detail?.settings?.zone
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        SectionLabel(text = stringResource(Res.string.settings_marking_title))
        ListGroup {
            NavigationRow(
                title = stringResource(Res.string.hotel_zone_title),
                subtitle = zone?.let { stringResource(Res.string.settings_hotel_zone_set, formatThousands(it.radiusMeters.toLong())) } ?: stringResource(Res.string.settings_hotel_zone_none),
                icon = MajesticIcons.myLocation,
                onClick = { onIntent(SettingsIntent.OpenHotelZone) },
            )
            ListDivider()
            NavigationRow(
                title = stringResource(Res.string.devices_title),
                subtitle = stringResource(Res.string.settings_devices_hint),
                icon = MajesticIcons.computer,
                onClick = { onIntent(SettingsIntent.OpenDevices) },
            )
        }
    }
}

@Composable
private fun NavigationRow(title: String, subtitle: String, icon: DrawableResource, onClick: () -> Unit) {
    ListRow(
        title = title,
        subtitle = subtitle,
        leading = { MajesticIcon(icon = icon, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        trailing = { MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        onClick = onClick,
    )
}

/** RF-EMP-07: the backup is written by the desktop app. */
@Composable
private fun Backup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    SectionCard(
        title = stringResource(Res.string.settings_backup_title),
        help = stringResource(if (state.backupAvailable) Res.string.settings_backup_hint else Res.string.settings_backup_only_desktop),
    ) {
        if (!state.backupAvailable) return@SectionCard
        // Decision 6A of M11 and RNF-14: the file has the data of the guests.
        Banner(tone = BannerTone.Warning, icon = MajesticIcons.warning, message = stringResource(Res.string.settings_backup_personal_data))
        MajesticTextField(
            value = state.backupSinceText,
            onValueChange = { onIntent(SettingsIntent.BackupSinceChange(it)) },
            label = stringResource(Res.string.settings_backup_date),
            placeholder = BACKUP_DATE_EXAMPLE,
            help = stringResource(Res.string.settings_backup_date_hint),
            error = if (state.showBackupDateError) stringResource(Res.string.settings_backup_date_error) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        MajesticButton(
            text = stringResource(Res.string.settings_backup_export),
            onClick = { onIntent(SettingsIntent.ExportBackup) },
            style = ButtonStyle.Outline,
            icon = MajesticIcons.backup,
            enabled = !state.isWorking,
            loading = state.isExporting,
            modifier = Modifier.fillMaxWidth(),
        )
        state.lastBackup?.let { backup ->
            Banner(tone = BannerTone.Info, icon = MajesticIcons.checkCircle, message = stringResource(Res.string.settings_backup_done, backup.documentCount, backup.filePath))
        }
    }
}

private const val BACKUP_DATE_EXAMPLE = "2026-11-01"

private suspend fun SettingsEffect.message(): String? = when (this) {
    SettingsEffect.NavigateToHotelZone, SettingsEffect.NavigateToDevices -> null
    SettingsEffect.Saved -> getString(Res.string.settings_saved)
    is SettingsEffect.SaveFailed -> getString(
        when (error) {
            SettingsError.InvalidTime -> Res.string.settings_time_error
            SettingsError.InvalidAttendanceRules -> Res.string.settings_attendance_error
            SettingsError.InvalidZone -> Res.string.hotel_zone_error
            SettingsError.NoSession -> Res.string.order_error_no_session
            SettingsError.NotAllowed -> Res.string.settings_not_allowed
            SettingsError.Network -> Res.string.employee_error_network
            SettingsError.Unknown -> Res.string.employee_error_unknown
        },
    )
    is SettingsEffect.BackupFailed -> getString(
        when (error) {
            BackupError.NotAvailable -> Res.string.settings_backup_only_desktop
            BackupError.InvalidDate -> Res.string.settings_backup_date_error
            BackupError.NotAllowed -> Res.string.settings_backup_not_allowed
            BackupError.Network -> Res.string.employee_error_network
            BackupError.CannotWriteFile -> Res.string.settings_backup_error_file
            BackupError.Unknown -> Res.string.employee_error_unknown
        },
    )
}
