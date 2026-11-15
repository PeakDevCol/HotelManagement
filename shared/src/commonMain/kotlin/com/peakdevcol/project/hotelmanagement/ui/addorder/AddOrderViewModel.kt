package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.totalAmount
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.OrderError
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What happened when the employee confirmed the order. Shown in a dialog until dismissed. */
sealed interface SaveResult {
    data object Sent : SaveResult

    /** RF-ORD-09: there was no answer from the server, so the order waits on the device. */
    data object SavedOnDevice : SaveResult
    data class Failed(val reason: SaveFailure) : SaveResult
}

enum class SaveFailure { NoSession, Unknown }

data class AddOrderState(
    val catalog: List<Product> = emptyList(),
    val catalogFailed: Boolean = false,
    val products: List<Product> = emptyList(),
    val destination: OrderDestination? = null,
    val isSaving: Boolean = false,
    val saveResult: SaveResult? = null,
) {
    val totalAmount: Long get() = products.totalAmount()
    val canConfirm: Boolean get() = products.isNotEmpty() && destination != null && !isSaving
}

sealed interface AddOrderIntent {
    data object RetryCatalog : AddOrderIntent
    data class AddProduct(val product: Product, val quantity: Int) : AddOrderIntent
    data class RemoveProduct(val productId: String) : AddOrderIntent
    data class SelectDestination(val destination: OrderDestination) : AddOrderIntent
    data object Confirm : AddOrderIntent
    data object DismissResult : AddOrderIntent
}

class AddOrderViewModel(
    private val getProducts: GetProductsUseCase,
    private val saveOrder: SaveOrderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddOrderState())
    val state: StateFlow<AddOrderState> = _state.asStateFlow()

    // Kept between retries so a save that reached the server is overwritten, not duplicated.
    private var draftOrderId = Order.newId()

    init {
        loadCatalog()
    }

    fun onIntent(intent: AddOrderIntent) {
        when (intent) {
            AddOrderIntent.RetryCatalog -> loadCatalog()
            is AddOrderIntent.AddProduct -> addProduct(intent.product, intent.quantity)
            is AddOrderIntent.RemoveProduct -> removeProduct(intent.productId)
            is AddOrderIntent.SelectDestination -> _state.update { it.copy(destination = intent.destination) }
            AddOrderIntent.Confirm -> confirm()
            AddOrderIntent.DismissResult -> _state.update { it.copy(saveResult = null) }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            getProducts()
                .onSuccess { catalog -> _state.update { it.copy(catalog = catalog, catalogFailed = false) } }
                .onFailure { _state.update { it.copy(catalogFailed = true) } }
        }
    }

    /** Adding a product that is already in the order increases its quantity. */
    private fun addProduct(product: Product, quantity: Int) {
        if (quantity <= 0) return
        _state.update { state ->
            val alreadyInOrder = state.products.any { it.id == product.id }
            val products = if (alreadyInOrder) {
                state.products.map { if (it.id == product.id) it.copy(quantity = it.quantity + quantity) else it }
            } else {
                state.products + product.copy(quantity = quantity)
            }
            state.copy(products = products)
        }
    }

    private fun removeProduct(productId: String) = _state.update { state ->
        state.copy(products = state.products.filterNot { it.id == productId })
    }

    private fun confirm() {
        val state = _state.value
        val destination = state.destination ?: return
        if (!state.canConfirm) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = saveOrder(state.products, destination, orderId = draftOrderId)
            _state.update { current ->
                result.fold(
                    onSuccess = { delivery ->
                        draftOrderId = Order.newId()
                        AddOrderState(catalog = current.catalog, saveResult = delivery.toSaveResult())
                    },
                    onFailure = { error ->
                        current.copy(isSaving = false, saveResult = SaveResult.Failed(error.toSaveFailure()))
                    },
                )
            }
        }
    }

    private fun OrderDelivery.toSaveResult(): SaveResult = when (this) {
        OrderDelivery.SENT -> SaveResult.Sent
        OrderDelivery.PENDING -> SaveResult.SavedOnDevice
    }

    private fun Throwable.toSaveFailure(): SaveFailure = when (this) {
        OrderError.NoSession -> SaveFailure.NoSession
        else -> SaveFailure.Unknown
    }
}
