package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.OrderFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.model.OrderOwnerFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenOrdersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class CurrentOrdersState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val orders: List<OrderListing> = emptyList(),
    val filter: OrderFilter = OrderFilter(),
    /** On the phone the search field opens from the top bar. */
    val isSearchOpen: Boolean = false,
    /** The order open beside the list, on wide windows. */
    val selectedOrderId: String? = null,
    /** To say how long ago each order was taken. It moves every minute. */
    val now: Instant = Instant.DISTANT_PAST,
) {
    val visibleOrders: List<OrderListing> get() = orders.filter(filter::matches)

    /** RF-ORD-09: orders saved on a device that the server does not have yet. */
    val pendingToSendCount: Int get() = orders.count { it.order.isPendingToSend }

    /** How many orders a filter chip would show, with the other filters as they are. */
    fun countWith(filter: OrderFilter): Int = orders.count(filter::matches)
}

sealed interface CurrentOrdersIntent {
    data class SelectOwner(val owner: OrderOwnerFilter) : CurrentOrdersIntent

    /** Choosing the state already chosen shows every state again. */
    data class ToggleState(val state: OrderState) : CurrentOrdersIntent
    data object OpenSearch : CurrentOrdersIntent
    data class Search(val query: String) : CurrentOrdersIntent
    data object CloseSearch : CurrentOrdersIntent

    /** [besideList] when the window shows the detail next to the list instead of on its own screen. */
    data class OpenOrder(val orderId: String, val besideList: Boolean) : CurrentOrdersIntent
    data object NewOrder : CurrentOrdersIntent
    data object Retry : CurrentOrdersIntent
}

sealed interface CurrentOrdersEffect {
    data class NavigateToOrder(val orderId: String) : CurrentOrdersEffect
    data object NavigateToNewOrder : CurrentOrdersEffect
}

/** The time once a minute, so "Hace 12 min" stays right while the list is open. */
fun everyMinute(clock: Clock = Clock.System): Flow<Instant> = flow {
    while (true) {
        emit(clock.now())
        delay(1.minutes)
    }
}

/** RF-CUR-01 and RF-CUR-02. */
class CurrentOrdersViewModel(
    private val observeOpenOrders: ObserveOpenOrdersUseCase,
    minuteTicks: Flow<Instant> = everyMinute(),
) : ViewModel() {

    private val _state = MutableStateFlow(CurrentOrdersState())
    val state: StateFlow<CurrentOrdersState> = _state.asStateFlow()

    private val _effects = Channel<CurrentOrdersEffect>(Channel.BUFFERED)
    val effects: Flow<CurrentOrdersEffect> = _effects.receiveAsFlow()

    private var ordersJob: Job? = null

    init {
        listenToOrders()
        viewModelScope.launch {
            minuteTicks.collect { now -> _state.update { it.copy(now = now) } }
        }
    }

    fun onIntent(intent: CurrentOrdersIntent) {
        when (intent) {
            is CurrentOrdersIntent.SelectOwner -> filter { it.copy(owner = intent.owner) }
            is CurrentOrdersIntent.ToggleState -> filter { it.copy(state = intent.state.takeIf { state -> state != it.state }) }
            CurrentOrdersIntent.OpenSearch -> _state.update { it.copy(isSearchOpen = true) }
            is CurrentOrdersIntent.Search -> filter { it.copy(query = intent.query) }
            CurrentOrdersIntent.CloseSearch -> _state.update { it.copy(isSearchOpen = false, filter = it.filter.copy(query = "")) }
            is CurrentOrdersIntent.OpenOrder -> openOrder(intent.orderId, intent.besideList)
            CurrentOrdersIntent.NewOrder -> _effects.trySend(CurrentOrdersEffect.NavigateToNewOrder)
            CurrentOrdersIntent.Retry -> listenToOrders()
        }
    }

    private fun filter(change: (OrderFilter) -> OrderFilter) = _state.update { it.copy(filter = change(it.filter)) }

    private fun openOrder(orderId: String, besideList: Boolean) {
        if (besideList) {
            _state.update { it.copy(selectedOrderId = orderId) }
        } else {
            _effects.trySend(CurrentOrdersEffect.NavigateToOrder(orderId))
        }
    }

    private fun listenToOrders() {
        ordersJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        ordersJob = viewModelScope.launch {
            observeOpenOrders()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { orders -> _state.update { it.copy(isLoading = false, orders = orders) } }
        }
    }
}
