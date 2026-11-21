package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.MarkOutcome
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkAtReceptionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveReceptionMarkingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceptionMarkingState(
    /** RF-ASI-11: only an authorized computer shows marking for the employees. */
    val isAvailable: Boolean = false,
    /** Until the authorization is known, so the screen does not show the wrong card first. */
    val isChecking: Boolean = true,
    val emailText: String = "",
    val passwordText: String = "",
    val isPasswordVisible: Boolean = false,
    val isMarking: Boolean = false,
    val result: ReceptionResult? = null,
) {
    val canMark: Boolean get() = emailText.isNotBlank() && passwordText.isNotEmpty() && !isMarking
}

sealed interface ReceptionResult {
    data class Done(val outcome: MarkOutcome) : ReceptionResult
    data class Failed(val error: AttendanceError) : ReceptionResult
}

sealed interface ReceptionMarkingIntent {
    data class EmailChange(val text: String) : ReceptionMarkingIntent
    data class PasswordChange(val text: String) : ReceptionMarkingIntent
    data object TogglePasswordVisibility : ReceptionMarkingIntent
    data object Mark : ReceptionMarkingIntent
}

/**
 * RF-ASI-11: at the computer of reception, each employee marks with their own account. The
 * computer is shared, so nothing of one employee stays on the screen for the next one.
 */
class ReceptionMarkingViewModel(
    observeReceptionMarking: ObserveReceptionMarkingUseCase,
    private val markAtReception: MarkAtReceptionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceptionMarkingState())
    val state: StateFlow<ReceptionMarkingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeReceptionMarking().collect { available -> _state.update { it.copy(isAvailable = available, isChecking = false) } }
        }
    }

    fun onIntent(intent: ReceptionMarkingIntent) {
        when (intent) {
            // The next employee starts typing: the result of the one before goes away.
            is ReceptionMarkingIntent.EmailChange -> _state.update { it.copy(emailText = intent.text, result = null) }
            is ReceptionMarkingIntent.PasswordChange -> _state.update { it.copy(passwordText = intent.text, result = null) }
            ReceptionMarkingIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            ReceptionMarkingIntent.Mark -> mark()
        }
    }

    private fun mark() {
        val state = _state.value
        if (!state.isAvailable || !state.canMark) return
        _state.update { it.copy(isMarking = true, result = null) }
        viewModelScope.launch {
            val outcome = markAtReception(state.emailText, state.passwordText)
            _state.update { current ->
                outcome.fold(
                    onSuccess = { current.copy(isMarking = false, emailText = "", passwordText = "", isPasswordVisible = false, result = ReceptionResult.Done(it)) },
                    onFailure = {
                        current.copy(
                            isMarking = false,
                            passwordText = "",
                            isPasswordVisible = false,
                            result = ReceptionResult.Failed(it as? AttendanceError ?: AttendanceError.Unknown),
                        )
                    },
                )
            }
        }
    }
}
