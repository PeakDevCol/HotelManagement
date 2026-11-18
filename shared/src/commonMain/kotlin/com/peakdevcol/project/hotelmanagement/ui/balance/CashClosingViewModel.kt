package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingOverview
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCashClosingUseCase
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

data class CashClosingState(
    val isLoading: Boolean = true,
    val loadError: CashClosingError? = null,
    val overview: CashClosingOverview? = null,
    val employeeNames: Map<String, String> = emptyMap(),
    val type: CashClosingType? = null,
    /** The counted cash as it is being typed, only digits. */
    val countedText: String = "",
    val note: String = "",
    /** Why the input cannot be saved yet, shown next to its field. */
    val inputError: CashClosingError? = null,
    val isConfirming: Boolean = false,
    val isSaving: Boolean = false,
) {
    val countedCash: Long? get() = countedText.toLongOrNull()

    /** Null until the counted cash is typed. */
    val difference: Long? get() = countedCash?.let { overview?.draft?.differenceFor(it) }
}

sealed interface CashClosingIntent {
    data object Reload : CashClosingIntent
    data class SelectType(val type: CashClosingType) : CashClosingIntent
    data class CountedChange(val text: String) : CashClosingIntent
    data class NoteChange(val text: String) : CashClosingIntent

    /** Asks for confirmation, because a saved closing is never edited. */
    data object RequestSave : CashClosingIntent
    data object ConfirmSave : CashClosingIntent
    data object DismissConfirmation : CashClosingIntent
}

sealed interface CashClosingEffect {
    data object Saved : CashClosingEffect
    data class SaveFailed(val error: CashClosingError) : CashClosingEffect
}

class CashClosingViewModel(
    private val loadCashClosing: LoadCashClosingUseCase,
    private val saveCashClosing: SaveCashClosingUseCase,
    observeEmployees: ObserveEmployeesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CashClosingState())
    val state: StateFlow<CashClosingState> = _state.asStateFlow()

    private val _effects = Channel<CashClosingEffect>(Channel.BUFFERED)
    val effects: Flow<CashClosingEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeEmployees()
                .catch { emit(emptyList()) }
                .collect { employees -> _state.update { it.copy(employeeNames = employees.associate { e -> e.id to e.fullName }) } }
        }
        load()
    }

    fun onIntent(intent: CashClosingIntent) {
        when (intent) {
            CashClosingIntent.Reload -> load()
            is CashClosingIntent.SelectType -> _state.update { it.copy(type = intent.type, inputError = null) }
            is CashClosingIntent.CountedChange ->
                _state.update { it.copy(countedText = intent.text.filter(Char::isDigit).take(MAX_DIGITS), inputError = null) }
            is CashClosingIntent.NoteChange -> _state.update { it.copy(note = intent.text, inputError = null) }
            CashClosingIntent.RequestSave -> requestSave()
            CashClosingIntent.ConfirmSave -> save()
            CashClosingIntent.DismissConfirmation -> _state.update { it.copy(isConfirming = false) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            val result = loadCashClosing()
            _state.update {
                it.copy(
                    isLoading = false,
                    overview = result.getOrNull() ?: it.overview,
                    loadError = result.exceptionOrNull()?.let { error -> error as? CashClosingError ?: CashClosingError.Unknown },
                )
            }
        }
    }

    private fun requestSave() {
        val state = _state.value
        val draft = state.overview?.draft ?: return
        if (state.isSaving || state.isLoading) return
        val error = draft.validate(state.type, state.countedCash, state.note)
        _state.update { it.copy(inputError = error, isConfirming = error == null) }
    }

    private fun save() {
        val state = _state.value
        val draft = state.overview?.draft ?: return
        if (state.isSaving) return
        _state.update { it.copy(isConfirming = false, isSaving = true) }
        viewModelScope.launch {
            val error = saveCashClosing(draft, state.type, state.countedCash, state.note)
                .exceptionOrNull()?.let { it as? CashClosingError ?: CashClosingError.Unknown }
            _state.update { it.copy(isSaving = false) }
            if (error == null) {
                _state.update { it.copy(type = null, countedText = "", note = "") }
                _effects.send(CashClosingEffect.Saved)
                load()
            } else {
                _effects.send(CashClosingEffect.SaveFailed(error))
                // Another administrator closed first: the next closing now starts somewhere else.
                if (error == CashClosingError.AlreadyClosed) load()
            }
        }
    }

    private companion object {
        // Up to $999.999.999.999, far from what a cash drawer holds and from the limits of Long.
        const val MAX_DIGITS = 12
    }
}
