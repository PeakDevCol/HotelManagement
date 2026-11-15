package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutOutcome
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LogOutDialog {
    Confirmation,

    /** RF-AUTH-03: there are orders waiting to be sent, so logging out is not allowed. */
    PendingOrders,
}

data class HomeState(
    val pendingOrderCount: Int = 0,
    val logOutDialog: LogOutDialog? = null,
)

sealed interface HomeIntent {
    data object LogOutClick : HomeIntent
    data object LogOutConfirm : HomeIntent
    data object LogOutDialogDismiss : HomeIntent
}

class HomeViewModel(
    observePendingOrderCount: ObservePendingOrderCountUseCase,
    private val logOut: LogOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Without the count the menu still works; logging out checks the pending orders again.
            observePendingOrderCount()
                .catch { emit(0) }
                .collect { count -> _state.update { it.copy(pendingOrderCount = count) } }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LogOutClick -> askBeforeLoggingOut()
            HomeIntent.LogOutConfirm -> confirmLogOut()
            HomeIntent.LogOutDialogDismiss -> _state.update { it.copy(logOutDialog = null) }
        }
    }

    private fun askBeforeLoggingOut() = _state.update { state ->
        val dialog = if (state.pendingOrderCount > 0) LogOutDialog.PendingOrders else LogOutDialog.Confirmation
        state.copy(logOutDialog = dialog)
    }

    /** On success the session change takes the app back to the introduction screen. */
    private fun confirmLogOut() {
        viewModelScope.launch {
            val dialog = when (logOut()) {
                LogOutOutcome.LOGGED_OUT -> null
                LogOutOutcome.BLOCKED_BY_PENDING_ORDERS -> LogOutDialog.PendingOrders
            }
            _state.update { it.copy(logOutDialog = dialog) }
        }
    }
}
