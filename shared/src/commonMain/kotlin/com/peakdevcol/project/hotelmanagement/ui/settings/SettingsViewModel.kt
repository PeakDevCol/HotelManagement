package com.peakdevcol.project.hotelmanagement.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettingsDetail
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.usecase.ExportBackupUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelSettingsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveAttendanceRulesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOperationalDayStartUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val detail: HotelSettingsDetail? = null,
    /** The start of the operational day as it is being typed, on a 24-hour clock. */
    val hourText: String = "",
    val minuteText: String = "",
    val showTimeError: Boolean = false,
    /** RF-ASI-06 and RF-ASI-08, as they are being typed. */
    val toleranceText: String = "",
    val maxOpenHoursText: String = "",
    val showAttendanceError: Boolean = false,
    val isSaving: Boolean = false,
    val isSavingRules: Boolean = false,
    /** RF-EMP-07: only the desktop app writes backups. */
    val backupAvailable: Boolean = false,
    /** A date like "2026-11-01", or blank for a whole backup. */
    val backupSinceText: String = "",
    val showBackupDateError: Boolean = false,
    val isExporting: Boolean = false,
    val lastBackup: BackupSummary? = null,
) {
    val isWorking: Boolean get() = isSaving || isSavingRules || isExporting
}

sealed interface SettingsIntent {
    data class HourChange(val text: String) : SettingsIntent
    data class MinuteChange(val text: String) : SettingsIntent
    data object SaveOperationalDayStart : SettingsIntent
    data class ToleranceChange(val text: String) : SettingsIntent
    data class MaxOpenHoursChange(val text: String) : SettingsIntent
    data object SaveAttendanceRules : SettingsIntent
    data class BackupSinceChange(val text: String) : SettingsIntent
    data object ExportBackup : SettingsIntent
    data object OpenHotelZone : SettingsIntent
    data object OpenDevices : SettingsIntent
}

sealed interface SettingsEffect {
    data object Saved : SettingsEffect
    data class SaveFailed(val error: SettingsError) : SettingsEffect
    data class BackupFailed(val error: BackupError) : SettingsEffect
    data object NavigateToHotelZone : SettingsEffect
    data object NavigateToDevices : SettingsEffect
}

class SettingsViewModel(
    observeHotelSettings: ObserveHotelSettingsUseCase,
    private val saveOperationalDayStart: SaveOperationalDayStartUseCase,
    private val saveAttendanceRules: SaveAttendanceRulesUseCase,
    private val exportBackup: ExportBackupUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(backupAvailable = exportBackup.isAvailable))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeHotelSettings()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { detail -> _state.update { it.showing(detail) } }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.HourChange -> _state.update { it.copy(hourText = intent.text.digits(), showTimeError = false) }
            is SettingsIntent.MinuteChange -> _state.update { it.copy(minuteText = intent.text.digits(), showTimeError = false) }
            SettingsIntent.SaveOperationalDayStart -> save()
            is SettingsIntent.ToleranceChange ->
                _state.update { it.copy(toleranceText = intent.text.filter(Char::isDigit).take(3), showAttendanceError = false) }
            is SettingsIntent.MaxOpenHoursChange ->
                _state.update { it.copy(maxOpenHoursText = intent.text.filter(Char::isDigit).take(2), showAttendanceError = false) }
            SettingsIntent.SaveAttendanceRules -> saveRules()
            is SettingsIntent.BackupSinceChange ->
                _state.update { it.copy(backupSinceText = intent.text.take(MAX_DATE_LENGTH), showBackupDateError = false) }
            SettingsIntent.ExportBackup -> export()
            SettingsIntent.OpenHotelZone -> _effects.trySend(SettingsEffect.NavigateToHotelZone)
            SettingsIntent.OpenDevices -> _effects.trySend(SettingsEffect.NavigateToDevices)
        }
    }

    // The fields are filled the first time only: a change from the server must not overwrite what is being typed.
    private fun SettingsState.showing(detail: HotelSettingsDetail): SettingsState {
        val start = detail.settings.operationalDayStartMinutes
        return if (this.detail == null) {
            copy(
                isLoading = false,
                detail = detail,
                hourText = (start / 60).toString(),
                minuteText = (start % 60).toString().padStart(2, '0'),
                toleranceText = detail.settings.lateToleranceMinutes.toString(),
                maxOpenHoursText = detail.settings.maxOpenEntryHours.toString(),
            )
        } else {
            copy(detail = detail)
        }
    }

    private fun String.digits() = filter(Char::isDigit).take(2)

    private fun save() {
        val state = _state.value
        if (state.isWorking || state.detail == null) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val error = saveOperationalDayStart(state.hourText.toIntOrNull(), state.minuteText.toIntOrNull())
                .exceptionOrNull()?.let { it as? SettingsError ?: SettingsError.Unknown }
            _state.update { it.copy(isSaving = false, showTimeError = error == SettingsError.InvalidTime) }
            when (error) {
                null -> _effects.send(SettingsEffect.Saved)
                SettingsError.InvalidTime -> Unit
                else -> _effects.send(SettingsEffect.SaveFailed(error))
            }
        }
    }

    private fun saveRules() {
        val state = _state.value
        if (state.isWorking || state.detail == null) return
        _state.update { it.copy(isSavingRules = true) }
        viewModelScope.launch {
            val error = saveAttendanceRules(state.toleranceText.toIntOrNull(), state.maxOpenHoursText.toIntOrNull())
                .exceptionOrNull()?.let { it as? SettingsError ?: SettingsError.Unknown }
            _state.update { it.copy(isSavingRules = false, showAttendanceError = error == SettingsError.InvalidAttendanceRules) }
            when (error) {
                null -> _effects.send(SettingsEffect.Saved)
                SettingsError.InvalidAttendanceRules -> Unit
                else -> _effects.send(SettingsEffect.SaveFailed(error))
            }
        }
    }

    private fun export() {
        val state = _state.value
        if (state.isWorking || !state.backupAvailable) return
        _state.update { it.copy(isExporting = true, lastBackup = null) }
        viewModelScope.launch {
            val result = exportBackup(state.backupSinceText)
            val error = result.exceptionOrNull()?.let { it as? BackupError ?: BackupError.Unknown }
            _state.update {
                it.copy(isExporting = false, lastBackup = result.getOrNull(), showBackupDateError = error == BackupError.InvalidDate)
            }
            if (error != null && error != BackupError.InvalidDate) _effects.send(SettingsEffect.BackupFailed(error))
        }
    }

    private companion object {
        const val MAX_DATE_LENGTH = 10
    }
}
