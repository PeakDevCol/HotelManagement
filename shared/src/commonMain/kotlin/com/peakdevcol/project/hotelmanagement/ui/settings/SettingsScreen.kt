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
import com.peakdevcol.project.hotelmanagement.domain.model.formatTimeOfDay
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.employee_error_network
import com.peakdevcol.project.hotelmanagement.resources.employee_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.menu_settings
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_date
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_date_error
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_done
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_error_file
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_export
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_only_desktop
import com.peakdevcol.project.hotelmanagement.resources.settings_backup_title
import com.peakdevcol.project.hotelmanagement.resources.settings_day_start_hint
import com.peakdevcol.project.hotelmanagement.resources.settings_day_start_preview
import com.peakdevcol.project.hotelmanagement.resources.settings_day_start_title
import com.peakdevcol.project.hotelmanagement.resources.settings_error
import com.peakdevcol.project.hotelmanagement.resources.settings_hour
import com.peakdevcol.project.hotelmanagement.resources.settings_last_change
import com.peakdevcol.project.hotelmanagement.resources.settings_minute
import com.peakdevcol.project.hotelmanagement.resources.settings_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.settings_saved
import com.peakdevcol.project.hotelmanagement.resources.settings_time_error
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-EMP-06 and RF-EMP-07: the parameters of the hotel and the manual backup. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }

    SettingsContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun SettingsContent(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.menu_settings)) },
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
private fun TimeField(value: String, label: String, onChange: (String) -> Unit, hasError: Boolean, modifier: Modifier) {
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

/** RF-EMP-07: the backup is written by the desktop app. */
@Composable
private fun Backup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    SectionCard(
        title = stringResource(Res.string.settings_backup_title),
        help = stringResource(if (state.backupAvailable) Res.string.settings_backup_hint else Res.string.settings_backup_only_desktop),
    ) {
        if (!state.backupAvailable) return@SectionCard
        MajesticTextField(
            value = state.backupSinceText,
            onValueChange = { onIntent(SettingsIntent.BackupSinceChange(it)) },
            label = stringResource(Res.string.settings_backup_date),
            placeholder = BACKUP_DATE_EXAMPLE,
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

private suspend fun SettingsEffect.message(): String = when (this) {
    SettingsEffect.Saved -> getString(Res.string.settings_saved)
    is SettingsEffect.SaveFailed -> getString(
        when (error) {
            SettingsError.InvalidTime -> Res.string.settings_time_error
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
