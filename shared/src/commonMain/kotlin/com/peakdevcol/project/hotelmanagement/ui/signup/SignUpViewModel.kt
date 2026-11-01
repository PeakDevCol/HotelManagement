package com.peakdevcol.project.hotelmanagement.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateAccountUseCase
import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showFullNameError: Boolean = false,
    val showEmailError: Boolean = false,
    val showPasswordError: Boolean = false,
    val showConfirmPasswordError: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
) {
    val canSubmit: Boolean
        get() = fullName.isNotBlank() && email.isNotBlank() && password.isNotEmpty() &&
            confirmPassword.isNotEmpty() && !isLoading
}

class SignUpViewModel(private val createAccount: CreateAccountUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value).validatedWhileTyping() }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value).validatedWhileTyping() }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value).validatedWhileTyping() }
    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value).validatedWhileTyping() }

    fun onCreateAccountClick() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val validated = state.validated()
        if (validated.hasErrors) {
            _uiState.value = validated
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = createAccount(NewUser(fullName = state.fullName.trim(), email = state.email), state.password)
            // On success Firebase starts a session and the app moves to the home screen by itself.
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull() as? AuthError) }
        }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }

    // While typing, an empty field is not an error yet.
    private fun SignUpUiState.validatedWhileTyping() = copy(
        showFullNameError = fullName.isNotEmpty() && !UserInputValidator.isValidFullName(fullName),
        showEmailError = email.isNotEmpty() && !UserInputValidator.isValidEmail(email),
        showPasswordError = password.isNotEmpty() && !UserInputValidator.isValidPassword(password),
        showConfirmPasswordError = confirmPassword.isNotEmpty() && confirmPassword != password,
    )

    private fun SignUpUiState.validated() = copy(
        showFullNameError = !UserInputValidator.isValidFullName(fullName),
        showEmailError = !UserInputValidator.isValidEmail(email),
        showPasswordError = !UserInputValidator.isValidPassword(password),
        showConfirmPasswordError = confirmPassword != password,
    )

    private val SignUpUiState.hasErrors: Boolean
        get() = showFullNameError || showEmailError || showPasswordError || showConfirmPasswordError
}
