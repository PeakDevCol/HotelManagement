package com.peakdevcol.project.hotelmanagement.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.RoomTypeCatalog
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomTypesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomTypeUseCase
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

/** The type being created, or edited when [typeId] is not null. */
data class RoomTypeForm(
    val typeId: String? = null,
    val name: String = "",
    val capacityText: String = "",
    /** Pesos without decimals, as the administrator types them. */
    val rateDigits: String = "",
    val taxId: String? = null,
    val active: Boolean = true,
    /** The field the last attempt to save found wrong. */
    val error: RoomError? = null,
)

data class RoomTypesState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val catalog: RoomTypeCatalog = RoomTypeCatalog(),
    val form: RoomTypeForm? = null,
    val isSaving: Boolean = false,
) {
    /** An inactive tax is offered only to the type that already has it, so it can be seen and removed. */
    val selectableTaxes: List<Tax>
        get() {
            val currentTaxId = form?.typeId?.let { id -> catalog.types.firstOrNull { it.id == id }?.taxId }
            return catalog.taxes.filter { it.active || it.id == currentTaxId }
        }
}

sealed interface RoomTypesIntent {
    data object AddClick : RoomTypesIntent
    data class EditClick(val type: RoomType) : RoomTypesIntent
    data class NameChange(val name: String) : RoomTypesIntent
    data class CapacityChange(val text: String) : RoomTypesIntent
    data class RateChange(val digits: String) : RoomTypesIntent

    /** A null id leaves the type without tax. */
    data class TaxSelect(val taxId: String?) : RoomTypesIntent
    data class ActiveChange(val active: Boolean) : RoomTypesIntent
    data object Save : RoomTypesIntent
    data object DismissForm : RoomTypesIntent
    data object Retry : RoomTypesIntent
}

sealed interface RoomTypesEffect {
    data class Failed(val error: RoomError) : RoomTypesEffect
}

/** RF-HAB-01: the types of room, with their capacity, rate per night and optional tax (P27). */
class RoomTypesViewModel(
    private val observeRoomTypes: ObserveRoomTypesUseCase,
    private val saveRoomType: SaveRoomTypeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomTypesState())
    val state: StateFlow<RoomTypesState> = _state.asStateFlow()

    private val _effects = Channel<RoomTypesEffect>(Channel.BUFFERED)
    val effects: Flow<RoomTypesEffect> = _effects.receiveAsFlow()

    private var catalogJob: Job? = null

    init {
        listenToTypes()
    }

    fun onIntent(intent: RoomTypesIntent) {
        when (intent) {
            RoomTypesIntent.AddClick -> _state.update { it.copy(form = RoomTypeForm()) }
            is RoomTypesIntent.EditClick -> _state.update { it.copy(form = intent.type.toForm(it.catalog)) }
            is RoomTypesIntent.NameChange -> editForm { it.copy(name = intent.name.take(MAX_NAME_LENGTH)) }
            is RoomTypesIntent.CapacityChange -> editForm { it.copy(capacityText = intent.text.filter(Char::isDigit).take(MAX_CAPACITY_DIGITS)) }
            is RoomTypesIntent.RateChange -> editForm { it.copy(rateDigits = intent.digits.filter(Char::isDigit).take(MAX_RATE_DIGITS)) }
            is RoomTypesIntent.TaxSelect -> editForm { it.copy(taxId = intent.taxId) }
            is RoomTypesIntent.ActiveChange -> editForm { it.copy(active = intent.active) }
            RoomTypesIntent.Save -> save()
            RoomTypesIntent.DismissForm -> _state.update { it.copy(form = null) }
            RoomTypesIntent.Retry -> listenToTypes()
        }
    }

    private fun listenToTypes() {
        catalogJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        catalogJob = viewModelScope.launch {
            observeRoomTypes()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { catalog -> _state.update { it.copy(isLoading = false, catalog = catalog) } }
        }
    }

    // A tax that no longer exists is not kept: saving the type would point it to nothing.
    private fun RoomType.toForm(catalog: RoomTypeCatalog) = RoomTypeForm(
        typeId = id,
        name = name,
        capacityText = capacity.toString(),
        rateDigits = baseRate.toString(),
        taxId = taxId.takeIf { id -> catalog.taxes.any { it.id == id } },
        active = active,
    )

    private fun editForm(change: (RoomTypeForm) -> RoomTypeForm) = _state.update { state ->
        state.copy(form = state.form?.let { change(it).copy(error = null) })
    }

    // A wrong field keeps the form open with its error; anything else closes it.
    private fun save() {
        val form = _state.value.form ?: return
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val error = saveRoomType(
                typeId = form.typeId,
                name = form.name,
                capacity = form.capacityText.toIntOrNull(),
                baseRate = form.rateDigits.toLongOrNull(),
                taxId = form.taxId,
                active = form.active,
            ).exceptionOrNull()?.let { it as? RoomError ?: RoomError.Unknown }
            val fieldError = error.takeIf { it in FIELD_ERRORS }
            _state.update { it.copy(isSaving = false, form = fieldError?.let { form.copy(error = fieldError) }) }
            if (error != null && fieldError == null) _effects.send(RoomTypesEffect.Failed(error))
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
        const val MAX_CAPACITY_DIGITS = 2
        const val MAX_RATE_DIGITS = 9
        val FIELD_ERRORS = setOf(RoomError.BlankName, RoomError.DuplicateTypeName, RoomError.InvalidCapacity, RoomError.InvalidRate)
    }
}
