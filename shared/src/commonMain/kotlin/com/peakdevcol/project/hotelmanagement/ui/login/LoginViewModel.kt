package com.peakdevcol.project.hotelmanagement.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showEmailError: Boolean = false,
    val showPasswordError: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !isLoading
}

class LoginViewModel(private val login: LoginUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // While typing, an empty field is not an error yet.
    fun onEmailChange(email: String) = _uiState.update {
        it.copy(email = email, showEmailError = email.isNotEmpty() && !UserInputValidator.isValidEmail(email))
    }

    fun onPasswordChange(password: String) = _uiState.update {
        it.copy(
            password = password,
            showPasswordError = password.isNotEmpty() && !UserInputValidator.isValidPassword(password),
        )
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val emailValid = UserInputValidator.isValidEmail(state.email)
        val passwordValid = UserInputValidator.isValidPassword(state.password)
        if (!emailValid || !passwordValid) {
            _uiState.update { it.copy(showEmailError = !emailValid, showPasswordError = !passwordValid) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = login(state.email, state.password)
            // On success the session changes and the app moves to the home screen by itself.
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull() as? AuthError) }
        }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }
}
