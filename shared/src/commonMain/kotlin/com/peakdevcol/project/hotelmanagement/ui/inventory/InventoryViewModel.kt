package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
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

/** A product open beside the list, on wide windows. A null [productId] is a new product. */
data class OpenProduct(val productId: String?)

data class InventoryState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val inventory: Inventory = Inventory(categories = emptyList(), products = emptyList()),
    val openProduct: OpenProduct? = null,
) {
    /** A product always belongs to a category, so the first step of an empty inventory is to create one. */
    val needsCategories: Boolean get() = !isLoading && !loadFailed && inventory.categories.isEmpty()
    val canCreateProducts: Boolean get() = inventory.categories.isNotEmpty()
}

sealed interface InventoryIntent {
    /** [besideList] when the window shows the product next to the list instead of on its own screen. */
    data class NewProduct(val besideList: Boolean) : InventoryIntent
    data class SelectProduct(val productId: String, val besideList: Boolean) : InventoryIntent
    data object CloseProduct : InventoryIntent
    data object ManageCategories : InventoryIntent
    data object ManageTaxes : InventoryIntent
    data object Retry : InventoryIntent
}

sealed interface InventoryEffect {
    /** A null id opens the form to create a product. */
    data class NavigateToProduct(val productId: String?) : InventoryEffect
    data object NavigateToCategories : InventoryEffect
    data object NavigateToTaxes : InventoryEffect
}

class InventoryViewModel(
    private val observeInventory: ObserveInventoryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _effects = Channel<InventoryEffect>(Channel.BUFFERED)
    val effects: Flow<InventoryEffect> = _effects.receiveAsFlow()

    private var inventoryJob: Job? = null

    init {
        listenToInventory()
    }

    fun onIntent(intent: InventoryIntent) {
        when (intent) {
            is InventoryIntent.NewProduct -> if (_state.value.canCreateProducts) open(productId = null, besideList = intent.besideList)
            is InventoryIntent.SelectProduct -> open(intent.productId, intent.besideList)
            InventoryIntent.CloseProduct -> _state.update { it.copy(openProduct = null) }
            InventoryIntent.ManageCategories -> _effects.trySend(InventoryEffect.NavigateToCategories)
            InventoryIntent.ManageTaxes -> _effects.trySend(InventoryEffect.NavigateToTaxes)
            InventoryIntent.Retry -> listenToInventory()
        }
    }

    private fun open(productId: String?, besideList: Boolean) {
        if (besideList) {
            _state.update { it.copy(openProduct = OpenProduct(productId)) }
        } else {
            _effects.trySend(InventoryEffect.NavigateToProduct(productId))
        }
    }

    private fun listenToInventory() {
        inventoryJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        inventoryJob = viewModelScope.launch {
            observeInventory()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { inventory -> _state.update { it.copy(isLoading = false, inventory = inventory) } }
        }
    }
}
