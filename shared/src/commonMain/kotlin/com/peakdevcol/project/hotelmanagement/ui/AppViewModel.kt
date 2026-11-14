package com.peakdevcol.project.hotelmanagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.model.Access
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Loading : AppState
    data object FirebaseNotConfigured : AppState
    data object LoggedOut : AppState
    data object AccessDenied : AppState
    data class LoggedIn(val user: User) : AppState
    data class Error(val message: String) : AppState
}

sealed interface AppIntent {
    data object LogOut : AppIntent
}

/** Decides which part of the app is shown, based on the session and the employee profile. */
class AppViewModel(
    config: AppConfig,
    observeAccess: ObserveAccessUseCase,
    private val logOut: LogOutUseCase,
) : ViewModel() {

    val state: StateFlow<AppState> =
        if (!config.firebaseEnabled) {
            flowOf(AppState.FirebaseNotConfigured)
        } else {
            observeAccess().toAppState()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Loading)

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.LogOut -> viewModelScope.launch { logOut() }
        }
    }

    private fun Flow<Access>.toAppState(): Flow<AppState> =
        map { access ->
            when (access) {
                Access.NoSession -> AppState.LoggedOut
                Access.Checking -> AppState.Loading
                Access.Denied -> AppState.AccessDenied
                is Access.Granted -> AppState.LoggedIn(access.user)
            }
        }.catch { emit(AppState.Error(it.message ?: it.toString())) }
}
