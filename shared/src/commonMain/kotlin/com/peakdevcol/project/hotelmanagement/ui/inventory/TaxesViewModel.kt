package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.formatTaxRate
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveTaxUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The tax being created, or edited when [taxId] is not null. */
data class TaxForm(
    val taxId: String? = null,
    val name: String = "",
    /** The percentage as the administrator types it, like "19" or "8,5". */
    val rateText: String = "",
    val active: Boolean = true,
    val error: InventoryError? = null,
)

data class TaxesState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val taxes: List<Tax> = emptyList(),
    val form: TaxForm? = null,
    val isSaving: Boolean = false,
)

sealed interface TaxesIntent {
    data object AddClick : TaxesIntent
    data class EditClick(val tax: Tax) : TaxesIntent
    data class NameChange(val name: String) : TaxesIntent
    data class RateChange(val rateText: String) : TaxesIntent
    data class ActiveChange(val active: Boolean) : TaxesIntent
    data object Save : TaxesIntent
    data object DismissForm : TaxesIntent
}

sealed interface TaxesEffect {
    data class Failed(val error: InventoryError) : TaxesEffect
}

class TaxesViewModel(
    observeInventory: ObserveInventoryUseCase,
    private val saveTax: SaveTaxUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TaxesState())
    val state: StateFlow<TaxesState> = _state.asStateFlow()

    private val _effects = Channel<TaxesEffect>(Channel.BUFFERED)
    val effects: Flow<TaxesEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeInventory()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { inventory -> _state.update { it.copy(isLoading = false, taxes = inventory.taxes) } }
        }
    }

    fun onIntent(intent: TaxesIntent) {
        when (intent) {
            TaxesIntent.AddClick -> _state.update { it.copy(form = TaxForm()) }
            is TaxesIntent.EditClick -> _state.update { it.copy(form = intent.tax.toForm()) }
            is TaxesIntent.NameChange -> editForm { it.copy(name = intent.name.take(MAX_NAME_LENGTH)) }
            is TaxesIntent.RateChange -> editForm { it.copy(rateText = intent.rateText.filter(::isRateCharacter).take(MAX_RATE_LENGTH)) }
            is TaxesIntent.ActiveChange -> editForm { it.copy(active = intent.active) }
            TaxesIntent.Save -> save()
            TaxesIntent.DismissForm -> _state.update { it.copy(form = null) }
        }
    }

    private fun Tax.toForm() =
        TaxForm(taxId = id, name = name, rateText = formatTaxRate(rate).removeSuffix(" %"), active = active)

    private fun isRateCharacter(character: Char) = character.isDigit() || character == ',' || character == '.'

    private fun editForm(change: (TaxForm) -> TaxForm) = _state.update { state ->
        state.copy(form = state.form?.let { change(it).copy(error = null) })
    }

    // A wrong field keeps the form open with its error; anything else closes it.
    private fun save() {
        val form = _state.value.form ?: return
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val error = saveTax(form.taxId, form.name, form.rateText, form.active).exceptionOrNull()
                ?.let { it as? InventoryError ?: InventoryError.Unknown }
            val fieldError = error.takeIf { it in FIELD_ERRORS }
            _state.update { it.copy(isSaving = false, form = fieldError?.let { form.copy(error = fieldError) }) }
            if (error != null && fieldError == null) _effects.send(TaxesEffect.Failed(error))
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 30
        const val MAX_RATE_LENGTH = 6
        val FIELD_ERRORS = setOf(InventoryError.BlankName, InventoryError.InvalidTaxRate)
    }
}
