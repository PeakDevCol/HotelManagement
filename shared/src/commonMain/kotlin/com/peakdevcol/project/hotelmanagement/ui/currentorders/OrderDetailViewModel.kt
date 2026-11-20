package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.suggestedTip
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ChargeOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RemoveProductFromOrderUseCase
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

/** RF-CUR-05: which tip the guest leaves. */
enum class TipChoice {
    NONE,

    /** 10 % of the order without taxes (PRD, section 13, 2026-11-20). */
    SUGGESTED,
    OTHER,
}

/**
 * RF-CUR-05: the charge being filled in. The method is chosen every time, and "Sin propina" comes
 * marked, so a tip is only recorded when the guest pays it (PRD, section 13, 2026-11-20).
 */
data class ChargeForm(
    val method: PaymentMethod? = null,
    val tip: TipChoice = TipChoice.NONE,
    /** The pesos of "Otro valor", as they are typed. */
    val otherTipDigits: String = "",
)

sealed interface OrderDetailState {
    data object Loading : OrderDetailState
    data object NotFound : OrderDetailState
    data object LoadFailed : OrderDetailState

    data class Loaded(
        val detail: OrderDetail,
        val isWorking: Boolean = false,
        /** The change of state waiting for its confirmation: close, reopen or cancel. */
        val dialog: OrderAction? = null,
        val cancelReason: String = "",
        /** Open while the employee charges the order. */
        val charge: ChargeForm? = null,
        /** The line the employee asked to remove, waiting for the confirmation. */
        val productToRemove: Product? = null,
    ) : OrderDetailState {
        /** RF-CUR-12: changes of state need the server, so they wait while the order is not up to date. */
        val isOffline: Boolean get() = !detail.order.isUpToDate
        val canChangeState: Boolean get() = !isOffline && !isWorking
        val suggestedTip: Long get() = detail.order.suggestedTip

        /** Null while "Otro valor" has no amount yet. */
        val tipAmount: Long?
            get() = when (charge?.tip) {
                TipChoice.NONE, null -> 0
                TipChoice.SUGGESTED -> suggestedTip
                TipChoice.OTHER -> charge.otherTipDigits.toLongOrNull()
            }

        /** What the employee receives: the order and the tip. */
        val amountToReceive: Long get() = detail.order.totalAmount + (tipAmount ?: 0)
        val canSubmitCharge: Boolean get() = charge?.method != null && tipAmount != null && canChangeState
        val canSubmitCancellation: Boolean get() = cancelReason.isNotBlank() && canChangeState
    }
}

sealed interface OrderDetailIntent {
    data object Retry : OrderDetailIntent

    /** Charging opens its form; the other actions ask for a confirmation. */
    data class ActionClick(val action: OrderAction) : OrderDetailIntent
    data object DismissDialog : OrderDetailIntent
    data object ConfirmClose : OrderDetailIntent
    data object ConfirmReopen : OrderDetailIntent
    data class SelectPaymentMethod(val method: PaymentMethod) : OrderDetailIntent
    data class SelectTip(val tip: TipChoice) : OrderDetailIntent
    data class OtherTipChange(val digits: String) : OrderDetailIntent
    data object SubmitCharge : OrderDetailIntent
    data class CancelReasonChange(val reason: String) : OrderDetailIntent
    data object SubmitCancellation : OrderDetailIntent
    data class RemoveProductClick(val product: Product) : OrderDetailIntent
    data object ConfirmRemoveProduct : OrderDetailIntent
}

sealed interface OrderDetailEffect {
    data class ActionDone(val action: OrderAction) : OrderDetailEffect
    data class ActionFailed(val error: OrderActionError) : OrderDetailEffect
    data object ProductRemoved : OrderDetailEffect
}

/** RF-CUR-03 to RF-CUR-12. */
class OrderDetailViewModel(
    private val orderId: String,
    private val observeOrderDetail: ObserveOrderDetailUseCase,
    private val closeOrder: CloseOrderUseCase,
    private val reopenOrder: ReopenOrderUseCase,
    private val chargeOrder: ChargeOrderUseCase,
    private val cancelOrder: CancelOrderUseCase,
    private val removeProductFromOrder: RemoveProductFromOrderUseCase,
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
            OrderDetailIntent.DismissDialog -> updateLoaded { it.withoutDialogs() }
            OrderDetailIntent.ConfirmClose -> perform(OrderAction.CLOSE) { closeOrder(orderId) }
            OrderDetailIntent.ConfirmReopen -> perform(OrderAction.REOPEN) { reopenOrder(orderId) }
            is OrderDetailIntent.SelectPaymentMethod -> editCharge { it.copy(method = intent.method) }
            is OrderDetailIntent.SelectTip -> editCharge { it.copy(tip = intent.tip) }
            is OrderDetailIntent.OtherTipChange ->
                editCharge { it.copy(otherTipDigits = intent.digits.filter(Char::isDigit).take(MAX_TIP_DIGITS)) }
            OrderDetailIntent.SubmitCharge -> charge()
            is OrderDetailIntent.CancelReasonChange ->
                updateLoaded { it.copy(cancelReason = intent.reason.take(MAX_REASON_LENGTH)) }
            OrderDetailIntent.SubmitCancellation -> cancel()
            is OrderDetailIntent.RemoveProductClick -> updateLoaded { state ->
                if (state.detail.canRemoveProducts && !state.isWorking) state.copy(productToRemove = intent.product) else state
            }
            OrderDetailIntent.ConfirmRemoveProduct -> removeProduct()
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

    // A change coming from the server must not close what the employee is filling in.
    private fun OrderDetailState.showing(detail: OrderDetail?): OrderDetailState = when {
        detail == null -> OrderDetailState.NotFound
        this is OrderDetailState.Loaded -> copy(detail = detail)
        else -> OrderDetailState.Loaded(detail)
    }

    private fun askFor(action: OrderAction) = updateLoaded { state ->
        val offered = action in state.detail.availableActions && state.canChangeState
        when {
            !offered -> state
            action == OrderAction.PAY -> state.copy(charge = ChargeForm())
            else -> state.copy(dialog = action, cancelReason = "")
        }
    }

    private fun editCharge(change: (ChargeForm) -> ChargeForm) = updateLoaded { state ->
        state.charge?.let { state.copy(charge = change(it)) } ?: state
    }

    private fun charge() {
        val loaded = _state.value as? OrderDetailState.Loaded ?: return
        val method = loaded.charge?.method ?: return
        val tipAmount = loaded.tipAmount ?: return
        if (!loaded.canSubmitCharge) return
        perform(OrderAction.PAY) { chargeOrder(loaded.detail.order, method, tipAmount) }
    }

    private fun cancel() {
        val loaded = _state.value as? OrderDetailState.Loaded ?: return
        if (!loaded.canSubmitCancellation) return
        perform(OrderAction.CANCEL) { cancelOrder(orderId, loaded.cancelReason) }
    }

    private fun perform(action: OrderAction, change: suspend () -> Result<Unit>) =
        work { change().toEffect { OrderDetailEffect.ActionDone(action) } }

    private fun removeProduct() {
        val lineId = (_state.value as? OrderDetailState.Loaded)?.productToRemove?.lineId ?: return
        work { removeProductFromOrder(orderId, lineId).toEffect { OrderDetailEffect.ProductRemoved } }
    }

    private fun work(change: suspend () -> OrderDetailEffect) {
        val loaded = _state.value as? OrderDetailState.Loaded ?: return
        if (loaded.isWorking) return
        updateLoaded { it.withoutDialogs().copy(isWorking = true) }
        viewModelScope.launch {
            val effect = change()
            updateLoaded { it.copy(isWorking = false) }
            _effects.send(effect)
        }
    }

    private fun OrderDetailState.Loaded.withoutDialogs() = copy(dialog = null, charge = null, productToRemove = null)

    private fun Result<Unit>.toEffect(onSuccess: (Unit) -> OrderDetailEffect): OrderDetailEffect = fold(
        onSuccess = onSuccess,
        onFailure = { OrderDetailEffect.ActionFailed(it as? OrderActionError ?: OrderActionError.Unknown) },
    )

    private fun updateLoaded(change: (OrderDetailState.Loaded) -> OrderDetailState.Loaded) =
        _state.update { state -> if (state is OrderDetailState.Loaded) change(state) else state }

    private companion object {
        const val MAX_TIP_DIGITS = 7
        const val MAX_REASON_LENGTH = 200
    }
}
