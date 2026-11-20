package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSelection
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.usecase.AddProductsToOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddProductsState(
    val picker: ProductPickerState = ProductPickerState(),
    /** Null until the order is read, and when it no longer exists. */
    val order: Order? = null,
    val isSaving: Boolean = false,
) {
    /** RF-CUR-06: products are added only while the order is in progress. */
    val isOrderInProgress: Boolean get() = order?.state == OrderState.IN_PROGRESS
    val canConfirm: Boolean get() = isOrderInProgress && !picker.selection.isEmpty && !isSaving
}

sealed interface AddProductsIntent {
    data object RetryCatalog : AddProductsIntent
    data class Picker(val intent: ProductPickerIntent) : AddProductsIntent
    data object Confirm : AddProductsIntent
}

sealed interface AddProductsEffect {
    /** RF-CUR-13: without connection the products wait on the device. */
    data class Added(val delivery: OrderDelivery) : AddProductsEffect
    data class Failed(val error: OrderActionError) : AddProductsEffect
}

/** RF-CUR-06 and RF-CUR-13: adds products to an order in progress, each one as a new line. */
class AddProductsViewModel(
    private val orderId: String,
    observeOrder: ObserveOrderUseCase,
    private val getProducts: GetProductsUseCase,
    private val addProductsToOrder: AddProductsToOrderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProductsState())
    val state: StateFlow<AddProductsState> = _state.asStateFlow()

    private val _effects = Channel<AddProductsEffect>(Channel.BUFFERED)
    val effects: Flow<AddProductsEffect> = _effects.receiveAsFlow()

    init {
        loadCatalog()
        viewModelScope.launch {
            observeOrder(orderId)
                .catch { emit(null) }
                .collect { order -> _state.update { it.copy(order = order) } }
        }
    }

    fun onIntent(intent: AddProductsIntent) {
        when (intent) {
            AddProductsIntent.RetryCatalog -> loadCatalog()
            is AddProductsIntent.Picker -> _state.update { it.copy(picker = it.picker.after(intent.intent)) }
            AddProductsIntent.Confirm -> confirm()
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            getProducts()
                .onSuccess { catalog -> _state.update { it.copy(picker = it.picker.withCatalog(catalog)) } }
                .onFailure { _state.update { it.copy(picker = it.picker.copy(catalogFailed = true)) } }
        }
    }

    private fun confirm() {
        val state = _state.value
        val order = state.order ?: return
        if (!state.canConfirm) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = addProductsToOrder(order, state.picker.selection.products)
            _state.update { it.copy(isSaving = false) }
            result
                .onSuccess { delivery ->
                    _state.update { it.copy(picker = it.picker.copy(selection = ProductSelection())) }
                    _effects.send(AddProductsEffect.Added(delivery))
                }
                .onFailure { error -> _effects.send(AddProductsEffect.Failed(error as? OrderActionError ?: OrderActionError.Unknown)) }
        }
    }
}
