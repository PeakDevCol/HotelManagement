package com.peakdevcol.project.hotelmanagement.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface StartUiState {
    data object Loading : StartUiState
    data object FirebaseNotConfigured : StartUiState
    data object LoggedOut : StartUiState
    data class LoggedIn(val userId: String) : StartUiState
    data class Error(val message: String) : StartUiState
}

class StartViewModel(
    config: AppConfig,
    observeSession: ObserveSessionUseCase,
) : ViewModel() {

    val uiState: StateFlow<StartUiState> =
        if (!config.firebaseEnabled) {
            flowOf(StartUiState.FirebaseNotConfigured)
        } else {
            observeSession()
                .map { userId -> userId?.let(StartUiState::LoggedIn) ?: StartUiState.LoggedOut }
                .catch { emit(StartUiState.Error(it.message ?: it.toString())) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartUiState.Loading)
}
