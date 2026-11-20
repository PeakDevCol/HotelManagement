package com.peakdevcol.project.hotelmanagement.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.usecase.AuthorizeThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.IdentifyThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveDevicesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RevokeDeviceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val devices: List<AuthorizedDevice> = emptyList(),
    val thisDeviceId: String,
    /** RF-ASI-11: only the desktop app authorizes the computer it runs on. */
    val canAuthorizeThisDevice: Boolean,
    val nameText: String = "",
    val nameError: Boolean = false,
    val deviceToRevoke: AuthorizedDevice? = null,
    val isWorking: Boolean = false,
) {
    val thisDevice: AuthorizedDevice? get() = devices.firstOrNull { it.id == thisDeviceId && it.active }
}

sealed interface DevicesIntent {
    data class NameChange(val text: String) : DevicesIntent
    data object AuthorizeThisDevice : DevicesIntent
    data class Revoke(val device: AuthorizedDevice) : DevicesIntent
    data object ConfirmRevoke : DevicesIntent
    data object DismissRevoke : DevicesIntent
    data object Retry : DevicesIntent
}

sealed interface DevicesEffect {
    data object Authorized : DevicesEffect
    data object Revoked : DevicesEffect
    data class Failed(val error: AttendanceError) : DevicesEffect
}

/** RF-ASI-11: the computers of reception where attendance is marked. */
class DevicesViewModel(
    private val observeDevices: ObserveDevicesUseCase,
    private val authorizeThisDevice: AuthorizeThisDeviceUseCase,
    private val revokeDevice: RevokeDeviceUseCase,
    identifyThisDevice: IdentifyThisDeviceUseCase,
) : ViewModel() {

    private val identity = identifyThisDevice()
    private val _state = MutableStateFlow(DevicesState(thisDeviceId = identity.id, canAuthorizeThisDevice = identity.canMarkForOthers))
    val state: StateFlow<DevicesState> = _state.asStateFlow()

    private val _effects = Channel<DevicesEffect>(Channel.BUFFERED)
    val effects: Flow<DevicesEffect> = _effects.receiveAsFlow()

    private var devicesJob: Job? = null

    init {
        listenToDevices()
    }

    fun onIntent(intent: DevicesIntent) {
        when (intent) {
            is DevicesIntent.NameChange -> _state.update { it.copy(nameText = intent.text.take(MAX_NAME_LENGTH), nameError = false) }
            DevicesIntent.AuthorizeThisDevice -> authorize()
            is DevicesIntent.Revoke -> _state.update { it.copy(deviceToRevoke = intent.device) }
            DevicesIntent.ConfirmRevoke -> revoke()
            DevicesIntent.DismissRevoke -> _state.update { it.copy(deviceToRevoke = null) }
            DevicesIntent.Retry -> listenToDevices()
        }
    }

    private fun listenToDevices() {
        devicesJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        devicesJob = viewModelScope.launch {
            observeDevices()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { devices -> _state.update { it.copy(isLoading = false, devices = devices) } }
        }
    }

    private fun authorize() {
        val name = _state.value.nameText
        work {
            val error = authorizeThisDevice(name).exceptionOrNull()?.toAttendanceError()
            when (error) {
                null -> {
                    _state.update { it.copy(nameText = "") }
                    DevicesEffect.Authorized
                }
                AttendanceError.InvalidName -> {
                    _state.update { it.copy(nameError = true) }
                    null
                }
                else -> DevicesEffect.Failed(error)
            }
        }
    }

    private fun revoke() {
        val device = _state.value.deviceToRevoke ?: return
        _state.update { it.copy(deviceToRevoke = null) }
        work { revokeDevice(device.id).fold({ DevicesEffect.Revoked }, { DevicesEffect.Failed(it.toAttendanceError()) }) }
    }

    private fun work(action: suspend () -> DevicesEffect?) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val effect = action()
            _state.update { it.copy(isWorking = false) }
            effect?.let { _effects.send(it) }
        }
    }

    private fun Throwable.toAttendanceError() = this as? AttendanceError ?: AttendanceError.Unknown

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
