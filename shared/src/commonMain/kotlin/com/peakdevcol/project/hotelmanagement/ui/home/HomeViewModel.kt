package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Access
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
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

data class HomeState(
    val pendingOrderCount: Int = 0,
    val logOutDialog: LogOutDialog? = null,
    /** RF-CUR-13: products added without connection that the server did not accept. */
    val rejectedAdditions: List<PendingAddition> = emptyList(),
    /** Only an administrator manages the inventory (PRD, section 3). */
    val canManageInventory: Boolean = false,
)

sealed interface HomeIntent {
    data object LogOutClick : HomeIntent
    data object LogOutConfirm : HomeIntent
    data object LogOutDialogDismiss : HomeIntent
    data object RejectedAdditionsRead : HomeIntent
}

class HomeViewModel(
    observePendingOrderCount: ObservePendingOrderCountUseCase,
    observeAccess: ObserveAccessUseCase,
    observeRejectedAdditions: ObserveRejectedAdditionsUseCase,
    reconcilePendingAdditions: ReconcilePendingAdditionsUseCase,
    private val dismissRejectedAdditions: DismissRejectedAdditionsUseCase,
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

    init {
        // The menu is what opens after starting the app, so this is where it finds out what the
        // server did with the products that were added without connection.
        viewModelScope.launch { runCatching { reconcilePendingAdditions() } }
        viewModelScope.launch {
            observeAccess()
                .catch { emit(Access.Denied) }
                .collect { access ->
                    val isAdministrator = (access as? Access.Granted)?.user?.type == UserType.ADMIN
                    _state.update { it.copy(canManageInventory = isAdministrator) }
                }
        }
        viewModelScope.launch {
            observeRejectedAdditions()
                .catch { emit(emptyList()) }
                .collect { rejected -> _state.update { it.copy(rejectedAdditions = rejected) } }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LogOutClick -> askBeforeLoggingOut()
            HomeIntent.LogOutConfirm -> confirmLogOut()
            HomeIntent.LogOutDialogDismiss -> _state.update { it.copy(logOutDialog = null) }
            HomeIntent.RejectedAdditionsRead -> viewModelScope.launch {
                dismissRejectedAdditions(_state.value.rejectedAdditions)
            }
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
