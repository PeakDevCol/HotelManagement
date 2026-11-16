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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurrentOrdersState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val orders: List<OrderListing> = emptyList(),
    val filter: OrderFilter = OrderFilter(),
) {
    val visibleOrders: List<OrderListing> get() = orders.filter(filter::matches)
}

sealed interface CurrentOrdersIntent {
    data class SelectOwner(val owner: OrderOwnerFilter) : CurrentOrdersIntent

    /** Null shows every state. */
    data class SelectState(val state: OrderState?) : CurrentOrdersIntent
    data class OpenOrder(val orderId: String) : CurrentOrdersIntent
    data object Retry : CurrentOrdersIntent
}

sealed interface CurrentOrdersEffect {
    data class NavigateToOrder(val orderId: String) : CurrentOrdersEffect
}

class CurrentOrdersViewModel(
    private val observeOpenOrders: ObserveOpenOrdersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CurrentOrdersState())
    val state: StateFlow<CurrentOrdersState> = _state.asStateFlow()

    private val _effects = Channel<CurrentOrdersEffect>(Channel.BUFFERED)
    val effects: Flow<CurrentOrdersEffect> = _effects.receiveAsFlow()

    private var ordersJob: Job? = null

    init {
        listenToOrders()
    }

    fun onIntent(intent: CurrentOrdersIntent) {
        when (intent) {
            is CurrentOrdersIntent.SelectOwner -> _state.update { it.copy(filter = it.filter.copy(owner = intent.owner)) }
            is CurrentOrdersIntent.SelectState -> _state.update { it.copy(filter = it.filter.copy(state = intent.state)) }
            is CurrentOrdersIntent.OpenOrder -> _effects.trySend(CurrentOrdersEffect.NavigateToOrder(intent.orderId))
            CurrentOrdersIntent.Retry -> listenToOrders()
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
