package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Order
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

/** What happened when the employee confirmed the order. Shown once in a dialog. */
sealed interface SaveResult {
    data object Saved : SaveResult
    data class Failed(val reason: SaveFailure) : SaveResult
}

enum class SaveFailure { NoSession, Timeout, Unknown }

data class AddOrderUiState(
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

class AddOrderViewModel(
    private val getProducts: GetProductsUseCase,
    private val saveOrder: SaveOrderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddOrderUiState())
    val uiState: StateFlow<AddOrderUiState> = _uiState.asStateFlow()

    // Kept between retries so a save that reached the server is overwritten, not duplicated.
    private var draftOrderId = Order.newId()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            getProducts()
                .onSuccess { catalog -> _uiState.update { it.copy(catalog = catalog, catalogFailed = false) } }
                .onFailure { _uiState.update { it.copy(catalogFailed = true) } }
        }
    }

    /** Adding a product that is already in the order increases its quantity. */
    fun onAddProduct(product: Product, quantity: Int) {
        if (quantity <= 0) return
        _uiState.update { state ->
            val existing = state.products.firstOrNull { it.id == product.id }
            val products = if (existing == null) {
                state.products + product.copy(quantity = quantity)
            } else {
                state.products.map { if (it.id == product.id) it.copy(quantity = it.quantity + quantity) else it }
            }
            state.copy(products = products)
        }
    }

    fun onRemoveProduct(productId: String) = _uiState.update { state ->
        state.copy(products = state.products.filterNot { it.id == productId })
    }

    fun onDestinationSelected(destination: OrderDestination) = _uiState.update { it.copy(destination = destination) }

    fun onConfirm() {
        val state = _uiState.value
        val destination = state.destination ?: return
        if (!state.canConfirm) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = saveOrder(state.products, destination, orderId = draftOrderId)
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        draftOrderId = Order.newId()
                        AddOrderUiState(catalog = current.catalog, saveResult = SaveResult.Saved)
                    },
                    onFailure = { error ->
                        current.copy(isSaving = false, saveResult = SaveResult.Failed(error.toSaveFailure()))
                    },
                )
            }
        }
    }

    fun onResultDismissed() = _uiState.update { it.copy(saveResult = null) }

    private fun Throwable.toSaveFailure(): SaveFailure = when (this) {
        OrderError.NoSession -> SaveFailure.NoSession
        OrderError.Timeout -> SaveFailure.Timeout
        else -> SaveFailure.Unknown
    }
}
