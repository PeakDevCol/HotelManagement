package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Access
import com.peakdevcol.project.hotelmanagement.domain.model.AppNavigation
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.navigationFor
import com.peakdevcol.project.hotelmanagement.domain.usecase.DismissRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutOutcome
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReconcilePendingAdditionsUseCase
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

/** What every screen of a logged-in employee shares: the navigation, the session and the warnings. */
data class MainState(
    /** Null until the profile of the logged-in employee is read. */
    val employee: User? = null,
    /** RF-AUTH-05 and RF-EMP-05: the sections the role allows, and the ones of the area first. */
    val navigation: AppNavigation? = null,
    val pendingOrderCount: Int = 0,
    val logOutDialog: LogOutDialog? = null,
    /** RF-CUR-13: products added without connection that the server did not accept. */
    val rejectedAdditions: List<PendingAddition> = emptyList(),
)

sealed interface MainIntent {
    data object LogOutClick : MainIntent
    data object LogOutConfirm : MainIntent
    data object LogOutDialogDismiss : MainIntent
    data object RejectedAdditionsRead : MainIntent
}

class MainViewModel(
    observePendingOrderCount: ObservePendingOrderCountUseCase,
    observeAccess: ObserveAccessUseCase,
    observeRejectedAdditions: ObserveRejectedAdditionsUseCase,
    reconcilePendingAdditions: ReconcilePendingAdditionsUseCase,
    private val dismissRejectedAdditions: DismissRejectedAdditionsUseCase,
    private val logOut: LogOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Without the count the app still works; logging out checks the pending orders again.
            observePendingOrderCount()
                .catch { emit(0) }
                .collect { count -> _state.update { it.copy(pendingOrderCount = count) } }
        }
    }

    init {
        // This is what opens after starting the app, so this is where it finds out what the server
        // did with the products that were added without connection.
        viewModelScope.launch { runCatching { reconcilePendingAdditions() } }
        viewModelScope.launch {
            observeAccess()
                .catch { emit(Access.Denied) }
                .collect { access ->
                    val employee = (access as? Access.Granted)?.user
                    _state.update { it.copy(employee = employee, navigation = employee?.let(::navigationFor)) }
                }
        }
        viewModelScope.launch {
            observeRejectedAdditions()
                .catch { emit(emptyList()) }
                .collect { rejected -> _state.update { it.copy(rejectedAdditions = rejected) } }
        }
    }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.LogOutClick -> askBeforeLoggingOut()
            MainIntent.LogOutConfirm -> confirmLogOut()
            MainIntent.LogOutDialogDismiss -> _state.update { it.copy(logOutDialog = null) }
            MainIntent.RejectedAdditionsRead -> viewModelScope.launch {
                dismissRejectedAdditions(_state.value.rejectedAdditions)
            }
        }
    }

    private fun askBeforeLoggingOut() = _state.update { state ->
        val dialog = if (state.pendingOrderCount > 0) LogOutDialog.PendingOrders else LogOutDialog.Confirmation
        state.copy(logOutDialog = dialog)
    }

    /** On success the session change takes the app back to the login. */
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
