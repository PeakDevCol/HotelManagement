package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationDraft
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSelection
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.OrderError
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveFailure { NoSession, Unknown }

data class AddOrderState(
    val picker: ProductPickerState = ProductPickerState(),
    val destination: OrderDestination? = null,
    /** Open while the destination is being chosen. */
    val destinationDraft: DestinationDraft? = null,
    val isSaving: Boolean = false,
) {
    /** RF-ORD-04: an order is confirmed only with products and a destination. */
    val canConfirm: Boolean get() = !picker.selection.isEmpty && destination != null && !isSaving
}

sealed interface AddOrderIntent {
    data object RetryCatalog : AddOrderIntent
    data class Picker(val intent: ProductPickerIntent) : AddOrderIntent
    data object OpenDestination : AddOrderIntent
    data class SelectDestinationKind(val kind: DestinationKind) : AddOrderIntent
    data class DestinationNumberChange(val text: String) : AddOrderIntent
    data object ConfirmDestination : AddOrderIntent
    data object DismissDestination : AddOrderIntent

    /** Without a destination it opens the destination first: an order is never confirmed without one. */
    data object Confirm : AddOrderIntent
}

sealed interface AddOrderEffect {
    /** RF-ORD-09: [delivery] says whether the server has it or it waits on the device. */
    data class Saved(val delivery: OrderDelivery) : AddOrderEffect
    data class SaveFailed(val reason: SaveFailure) : AddOrderEffect
}

/** RF-ORD-01 to RF-ORD-05. */
class AddOrderViewModel(
    private val getProducts: GetProductsUseCase,
    private val saveOrder: SaveOrderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddOrderState())
    val state: StateFlow<AddOrderState> = _state.asStateFlow()

    private val _effects = Channel<AddOrderEffect>(Channel.BUFFERED)
    val effects: Flow<AddOrderEffect> = _effects.receiveAsFlow()

    // Kept between retries so a save that reached the server is overwritten, not duplicated.
    private var draftOrderId = Order.newId()

    init {
        loadCatalog()
    }

    fun onIntent(intent: AddOrderIntent) {
        when (intent) {
            AddOrderIntent.RetryCatalog -> loadCatalog()
            is AddOrderIntent.Picker -> _state.update { it.copy(picker = it.picker.after(intent.intent)) }
            AddOrderIntent.OpenDestination -> openDestination()
            is AddOrderIntent.SelectDestinationKind -> editDestination { it.copy(kind = intent.kind) }
            is AddOrderIntent.DestinationNumberChange ->
                editDestination { it.copy(numberText = intent.text.filter(Char::isDigit).take(MAX_NUMBER_DIGITS)) }
            AddOrderIntent.ConfirmDestination -> _state.update { state ->
                val destination = state.destinationDraft?.destination ?: return@update state
                state.copy(destination = destination, destinationDraft = null)
            }
            AddOrderIntent.DismissDestination -> _state.update { it.copy(destinationDraft = null) }
            AddOrderIntent.Confirm -> if (_state.value.destination == null) openDestination() else confirm()
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            getProducts()
                .onSuccess { catalog -> _state.update { it.copy(picker = it.picker.withCatalog(catalog)) } }
                .onFailure { _state.update { it.copy(picker = it.picker.copy(catalogFailed = true)) } }
        }
    }

    private fun openDestination() = _state.update { it.copy(destinationDraft = DestinationDraft.of(it.destination)) }

    private fun editDestination(change: (DestinationDraft) -> DestinationDraft) = _state.update { state ->
        state.destinationDraft?.let { state.copy(destinationDraft = change(it)) } ?: state
    }

    private fun confirm() {
        val state = _state.value
        val destination = state.destination ?: return
        if (!state.canConfirm) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveOrder(state.picker.selection.products, destination, orderId = draftOrderId)
            _state.update { it.copy(isSaving = false) }
            result
                .onSuccess { delivery ->
                    draftOrderId = Order.newId()
                    _state.update { it.copy(picker = it.picker.copy(selection = ProductSelection()), destination = null) }
                    _effects.send(AddOrderEffect.Saved(delivery))
                }
                .onFailure { error -> _effects.send(AddOrderEffect.SaveFailed(error.toSaveFailure())) }
        }
    }

    private fun Throwable.toSaveFailure(): SaveFailure = when (this) {
        OrderError.NoSession -> SaveFailure.NoSession
        else -> SaveFailure.Unknown
    }

    private companion object {
        const val MAX_NUMBER_DIGITS = 4
    }
}
