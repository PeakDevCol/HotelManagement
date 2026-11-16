package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface OrderDetailState {
    data object Loading : OrderDetailState
    data object NotFound : OrderDetailState
    data object LoadFailed : OrderDetailState
    data class Loaded(val detail: OrderDetail) : OrderDetailState
}

sealed interface OrderDetailIntent {
    data object Retry : OrderDetailIntent
}

class OrderDetailViewModel(
    private val orderId: String,
    private val observeOrderDetail: ObserveOrderDetailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<OrderDetailState>(OrderDetailState.Loading)
    val state: StateFlow<OrderDetailState> = _state.asStateFlow()

    private var detailJob: Job? = null

    init {
        listenToOrder()
    }

    fun onIntent(intent: OrderDetailIntent) {
        when (intent) {
            OrderDetailIntent.Retry -> listenToOrder()
        }
    }

    private fun listenToOrder() {
        detailJob?.cancel()
        _state.value = OrderDetailState.Loading
        detailJob = viewModelScope.launch {
            observeOrderDetail(orderId)
                .catch { _state.value = OrderDetailState.LoadFailed }
                .collect { detail ->
                    _state.value = detail?.let(OrderDetailState::Loaded) ?: OrderDetailState.NotFound
                }
        }
    }
}
