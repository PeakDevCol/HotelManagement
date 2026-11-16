package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterPaymentUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
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

sealed interface OrderDetailState {
    data object Loading : OrderDetailState
    data object NotFound : OrderDetailState
    data object LoadFailed : OrderDetailState

    data class Loaded(
        val detail: OrderDetail,
        val isWorking: Boolean = false,
        /** The action that is asking for a confirmation or for its data. */
        val dialog: OrderAction? = null,
    ) : OrderDetailState {
        /** RF-CUR-12: changes of state need the server, so they wait while the order is not up to date. */
        val isOffline: Boolean get() = !detail.order.isUpToDate
        val canChangeState: Boolean get() = !isOffline && !isWorking
    }
}

sealed interface OrderDetailIntent {
    data object Retry : OrderDetailIntent
    data class ActionClick(val action: OrderAction) : OrderDetailIntent
    data object DismissDialog : OrderDetailIntent
    data object ConfirmClose : OrderDetailIntent
    data object ConfirmReopen : OrderDetailIntent
    data class SubmitPayment(val method: PaymentMethod, val tipAmount: Long) : OrderDetailIntent
    data class SubmitCancellation(val reason: String) : OrderDetailIntent
}

sealed interface OrderDetailEffect {
    data class ActionDone(val action: OrderAction) : OrderDetailEffect
    data class ActionFailed(val error: OrderActionError) : OrderDetailEffect
}

class OrderDetailViewModel(
    private val orderId: String,
    private val observeOrderDetail: ObserveOrderDetailUseCase,
    private val closeOrder: CloseOrderUseCase,
    private val reopenOrder: ReopenOrderUseCase,
    private val registerPayment: RegisterPaymentUseCase,
    private val cancelOrder: CancelOrderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<OrderDetailState>(OrderDetailState.Loading)
    val state: StateFlow<OrderDetailState> = _state.asStateFlow()

    private val _effects = Channel<OrderDetailEffect>(Channel.BUFFERED)
    val effects: Flow<OrderDetailEffect> = _effects.receiveAsFlow()

    private var detailJob: Job? = null

    init {
        listenToOrder()
    }

    fun onIntent(intent: OrderDetailIntent) {
        when (intent) {
            OrderDetailIntent.Retry -> listenToOrder()
            is OrderDetailIntent.ActionClick -> askFor(intent.action)
            OrderDetailIntent.DismissDialog -> updateLoaded { it.copy(dialog = null) }
            OrderDetailIntent.ConfirmClose -> perform(OrderAction.CLOSE) { closeOrder(orderId) }
            OrderDetailIntent.ConfirmReopen -> perform(OrderAction.REOPEN) { reopenOrder(orderId) }
            is OrderDetailIntent.SubmitPayment ->
                perform(OrderAction.PAY) { registerPayment(orderId, intent.method, intent.tipAmount) }
            is OrderDetailIntent.SubmitCancellation ->
                perform(OrderAction.CANCEL) { cancelOrder(orderId, intent.reason) }
        }
    }

    private fun listenToOrder() {
        detailJob?.cancel()
        _state.value = OrderDetailState.Loading
        detailJob = viewModelScope.launch {
            observeOrderDetail(orderId)
                .catch { _state.value = OrderDetailState.LoadFailed }
                .collect { detail -> _state.update { it.showing(detail) } }
        }
    }

    // A change coming from the server must not close the dialog the employee is filling in.
    private fun OrderDetailState.showing(detail: OrderDetail?): OrderDetailState = when {
        detail == null -> OrderDetailState.NotFound
        this is OrderDetailState.Loaded -> copy(detail = detail)
        else -> OrderDetailState.Loaded(detail)
    }

    private fun askFor(action: OrderAction) = updateLoaded { state ->
        val offered = action in state.detail.availableActions && state.canChangeState
        if (offered) state.copy(dialog = action) else state
    }

    private fun perform(action: OrderAction, change: suspend () -> Result<Unit>) {
        val loaded = _state.value as? OrderDetailState.Loaded ?: return
        if (loaded.isWorking) return
        updateLoaded { it.copy(isWorking = true, dialog = null) }
        viewModelScope.launch {
            val effect = change().fold(
                onSuccess = { OrderDetailEffect.ActionDone(action) },
                onFailure = { OrderDetailEffect.ActionFailed(it as? OrderActionError ?: OrderActionError.Unknown) },
            )
            updateLoaded { it.copy(isWorking = false) }
            _effects.send(effect)
        }
    }

    private fun updateLoaded(change: (OrderDetailState.Loaded) -> OrderDetailState.Loaded) =
        _state.update { state -> if (state is OrderDetailState.Loaded) change(state) else state }
}
