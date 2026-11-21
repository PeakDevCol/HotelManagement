package com.peakdevcol.project.hotelmanagement.ui.hotelzone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.model.parseCoordinate
import com.peakdevcol.project.hotelmanagement.domain.model.parseCoordinatePair
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocateThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccess
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelZoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveHotelZoneUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HotelZoneState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** The fields as they are typed. */
    val latitudeText: String = "",
    val longitudeText: String = "",
    val radiusText: String = "",
    val accuracyText: String = "",
    val showError: Boolean = false,
    /** False on the desktop, which cannot tell where it is. */
    val canUseMyLocation: Boolean = false,
    /** S10: the app explains what the location is for before the system asks. */
    val isExplainingPermission: Boolean = false,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
) {
    val hasZone: Boolean get() = parseCoordinate(latitudeText) != null && parseCoordinate(longitudeText) != null
}

sealed interface HotelZoneIntent {
    /** A latitude, or the latitude and the longitude pasted together from a map. */
    data class LatitudeChange(val text: String) : HotelZoneIntent
    data class LongitudeChange(val text: String) : HotelZoneIntent
    data class RadiusChange(val text: String) : HotelZoneIntent
    data class AccuracyChange(val text: String) : HotelZoneIntent
    data object UseMyLocation : HotelZoneIntent
    data class PermissionResult(val granted: Boolean) : HotelZoneIntent
    data object DismissPermission : HotelZoneIntent
    data object Save : HotelZoneIntent
    data object Retry : HotelZoneIntent
}

sealed interface HotelZoneEffect {
    data object Saved : HotelZoneEffect

    /** The location of the device was put in the fields; it is saved with "Guardar". */
    data class Located(val accuracyMeters: Int) : HotelZoneEffect
    data class LocateFailed(val error: AttendanceError) : HotelZoneEffect
    data class SaveFailed(val error: SettingsError) : HotelZoneEffect
}

/** RF-ASI-10: an administrator fixes the zone of the hotel (PRD, section 13, 2026-11-20). */
class HotelZoneViewModel(
    private val observeHotelZone: ObserveHotelZoneUseCase,
    private val saveHotelZone: SaveHotelZoneUseCase,
    private val locateThisDevice: LocateThisDeviceUseCase,
    private val locationAccess: LocationAccessUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HotelZoneState(canUseMyLocation = locationAccess() != LocationAccess.UNAVAILABLE))
    val state: StateFlow<HotelZoneState> = _state.asStateFlow()

    private val _effects = Channel<HotelZoneEffect>(Channel.BUFFERED)
    val effects: Flow<HotelZoneEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: HotelZoneIntent) {
        when (intent) {
            is HotelZoneIntent.LatitudeChange -> changeLatitude(intent.text)
            is HotelZoneIntent.LongitudeChange -> edit { it.copy(longitudeText = intent.text.coordinateCharacters()) }
            is HotelZoneIntent.RadiusChange -> edit { it.copy(radiusText = intent.text.filter(Char::isDigit).take(MAX_METER_DIGITS)) }
            is HotelZoneIntent.AccuracyChange -> edit { it.copy(accuracyText = intent.text.filter(Char::isDigit).take(MAX_METER_DIGITS)) }
            HotelZoneIntent.UseMyLocation -> when (locationAccess()) {
                LocationAccess.GRANTED -> locate()
                LocationAccess.NEEDS_PERMISSION -> _state.update { it.copy(isExplainingPermission = true) }
                LocationAccess.UNAVAILABLE -> _effects.trySend(HotelZoneEffect.LocateFailed(AttendanceError.LocationUnavailable))
            }
            is HotelZoneIntent.PermissionResult -> {
                _state.update { it.copy(isExplainingPermission = false) }
                if (intent.granted) locate() else _effects.trySend(HotelZoneEffect.LocateFailed(AttendanceError.LocationPermissionDenied))
            }
            HotelZoneIntent.DismissPermission -> _state.update { it.copy(isExplainingPermission = false) }
            HotelZoneIntent.Save -> save()
            HotelZoneIntent.Retry -> load()
        }
    }

    // The fields are filled the first time only: a change from the server must not overwrite what is being typed.
    private fun load() {
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            observeHotelZone()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { settings -> _state.update { if (it.isLoading) it.filledWith(settings) else it } }
        }
    }

    private fun HotelZoneState.filledWith(settings: HotelSettings) = copy(
        isLoading = false,
        latitudeText = settings.hotelLatitude?.toString().orEmpty(),
        longitudeText = settings.hotelLongitude?.toString().orEmpty(),
        radiusText = settings.zoneRadiusMeters.toString(),
        accuracyText = settings.minAccuracyMeters.toString(),
    )

    private fun changeLatitude(text: String) {
        val pair = parseCoordinatePair(text)
        if (pair == null) {
            edit { it.copy(latitudeText = text.coordinateCharacters()) }
        } else {
            edit { it.copy(latitudeText = pair.first.toString(), longitudeText = pair.second.toString()) }
        }
    }

    private fun String.coordinateCharacters() = filter { it.isDigit() || it == '.' || it == ',' || it == '-' }.take(MAX_COORDINATE_LENGTH)

    private fun edit(change: (HotelZoneState) -> HotelZoneState) = _state.update { change(it).copy(showError = false) }

    private fun locate() {
        if (_state.value.isLocating) return
        _state.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val result = locateThisDevice()
            _state.update { it.copy(isLocating = false) }
            result
                .onSuccess { reading ->
                    edit { it.copy(latitudeText = reading.latitude.toString(), longitudeText = reading.longitude.toString()) }
                    _effects.send(HotelZoneEffect.Located(reading.accuracyMeters.toInt()))
                }
                .onFailure { _effects.send(HotelZoneEffect.LocateFailed(it as? AttendanceError ?: AttendanceError.Unknown)) }
        }
    }

    private fun save() {
        val state = _state.value
        if (state.isSaving || state.isLoading) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val error = saveHotelZone(
                parseCoordinate(state.latitudeText),
                parseCoordinate(state.longitudeText),
                state.radiusText.toIntOrNull(),
                state.accuracyText.toIntOrNull(),
            ).exceptionOrNull()?.let { it as? SettingsError ?: SettingsError.Unknown }
            _state.update { it.copy(isSaving = false, showError = error == SettingsError.InvalidZone) }
            when (error) {
                null -> _effects.send(HotelZoneEffect.Saved)
                SettingsError.InvalidZone -> Unit
                else -> _effects.send(HotelZoneEffect.SaveFailed(error))
            }
        }
    }

    private companion object {
        const val MAX_COORDINATE_LENGTH = 30
        const val MAX_METER_DIGITS = 4
    }
}
