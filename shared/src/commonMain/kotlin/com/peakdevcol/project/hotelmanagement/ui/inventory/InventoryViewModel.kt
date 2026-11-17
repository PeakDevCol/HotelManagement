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

data class InventoryState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val inventory: Inventory = Inventory(categories = emptyList(), products = emptyList()),
) {
    /** A product always belongs to a category, so the first step of an empty inventory is to create one. */
    val needsCategories: Boolean get() = !isLoading && !loadFailed && inventory.categories.isEmpty()
    val canCreateProducts: Boolean get() = inventory.categories.isNotEmpty()
}

sealed interface InventoryIntent {
    data object NewProduct : InventoryIntent
    data class OpenProduct(val productId: String) : InventoryIntent
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
            InventoryIntent.NewProduct ->
                if (_state.value.canCreateProducts) _effects.trySend(InventoryEffect.NavigateToProduct(productId = null))
            is InventoryIntent.OpenProduct -> _effects.trySend(InventoryEffect.NavigateToProduct(intent.productId))
            InventoryIntent.ManageCategories -> _effects.trySend(InventoryEffect.NavigateToCategories)
            InventoryIntent.ManageTaxes -> _effects.trySend(InventoryEffect.NavigateToTaxes)
            InventoryIntent.Retry -> listenToInventory()
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
