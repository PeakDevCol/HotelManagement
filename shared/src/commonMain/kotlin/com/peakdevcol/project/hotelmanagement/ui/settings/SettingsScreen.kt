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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatTimeOfDay
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.employee_error_network
import com.peakdevcol.project.hotelmanagement.resources.employee_error_unknown
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
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> snackbarHostState.showSnackbar(effect.message()) }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.menu_settings), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed -> CenteredMessage(stringResource(Res.string.settings_error), Modifier.padding(padding))
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OperationalDayStart(state = state, onIntent = viewModel::onIntent)
                    HorizontalDivider()
                    Backup(state = state, onIntent = viewModel::onIntent)
                }
            }
        }
    }

    if (state.isWorking) LoadingOverlay()
}

/** RF-EMP-06: the parameter of the v1.0. The following versions add theirs to this screen. */
@Composable
private fun OperationalDayStart(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val detail = state.detail ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.settings_day_start_title), style = MaterialTheme.typography.titleMedium)
        Hint(stringResource(Res.string.settings_day_start_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField(
                value = state.hourText,
                label = stringResource(Res.string.settings_hour),
                isError = state.showTimeError,
                onChange = { onIntent(SettingsIntent.HourChange(it)) },
                modifier = Modifier.weight(1f),
            )
            TimeField(
                value = state.minuteText,
                label = stringResource(Res.string.settings_minute),
                isError = state.showTimeError,
                onChange = { onIntent(SettingsIntent.MinuteChange(it)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.showTimeError) {
            Text(
                text = stringResource(Res.string.settings_time_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(Res.string.settings_day_start_preview, formatTimeOfDay(detail.settings.operationalDayStartMinutes)),
            style = MaterialTheme.typography.bodyLarge,
        )
        detail.settings.updatedAt?.let { updatedAt ->
            Hint(stringResource(Res.string.settings_last_change, employeeNameOrUnknown(detail.updatedByName), formatBogotaDateTime(updatedAt)))
        }
        PrimaryButton(
            text = stringResource(Res.string.save),
            onClick = { onIntent(SettingsIntent.SaveOperationalDayStart) },
            enabled = !state.isWorking,
        )
    }
}

@Composable
private fun TimeField(value: String, label: String, isError: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** RF-EMP-07: the backup is written by the desktop app. */
@Composable
private fun Backup(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.settings_backup_title), style = MaterialTheme.typography.titleMedium)
        if (!state.backupAvailable) {
            Hint(stringResource(Res.string.settings_backup_only_desktop))
            return@Column
        }
        Hint(stringResource(Res.string.settings_backup_hint))
        OutlinedTextField(
            value = state.backupSinceText,
            onValueChange = { onIntent(SettingsIntent.BackupSinceChange(it)) },
            label = { Text(stringResource(Res.string.settings_backup_date)) },
            placeholder = { Text("2026-11-01") },
            isError = state.showBackupDateError,
            supportingText = if (state.showBackupDateError) {
                { Text(stringResource(Res.string.settings_backup_date_error)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(Res.string.settings_backup_export),
            onClick = { onIntent(SettingsIntent.ExportBackup) },
            enabled = !state.isWorking,
        )
        state.lastBackup?.let { backup ->
            Text(
                text = stringResource(Res.string.settings_backup_done, backup.documentCount, backup.filePath),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

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
