package com.peakdevcol.project.hotelmanagement.ui

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

sealed interface SessionState {
    data object Loading : SessionState
    data object FirebaseNotConfigured : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val userId: String) : SessionState
    data class Error(val message: String) : SessionState
}

/** Decides which part of the app is shown, based on the Firebase session. */
class AppViewModel(
    config: AppConfig,
    observeSession: ObserveSessionUseCase,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> =
        if (!config.firebaseEnabled) {
            flowOf(SessionState.FirebaseNotConfigured)
        } else {
            observeSession()
                .map { userId -> userId?.let(SessionState::LoggedIn) ?: SessionState.LoggedOut }
                .catch { emit(SessionState.Error(it.message ?: it.toString())) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}
