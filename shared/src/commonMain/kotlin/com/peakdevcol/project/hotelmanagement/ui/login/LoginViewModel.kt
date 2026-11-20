package com.peakdevcol.project.hotelmanagement.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the login tells the employee, above the button, until they change a field. */
sealed interface LoginMessage {
    data class LoginFailed(val error: AuthError) : LoginMessage

    /** RF-AUTH-04: the same answer whether or not the email has an account. */
    data object PasswordEmailRequested : LoginMessage
    data object PasswordEmailFailed : LoginMessage
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val showEmailError: Boolean = false,
    val showPasswordError: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val message: LoginMessage? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !isLoading
}

sealed interface LoginIntent {
    data class EmailChange(val email: String) : LoginIntent
    data class PasswordChange(val password: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent
    data object LoginClick : LoginIntent
    data object ForgotPasswordClick : LoginIntent
    data object MessageDismiss : LoginIntent
}

class LoginViewModel(
    private val login: LoginUseCase,
    private val sendPasswordEmail: SendPasswordEmailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            // While typing, an empty field is not an error yet, and the last message no longer applies.
            is LoginIntent.EmailChange -> _state.update {
                val invalid = intent.email.isNotEmpty() && !UserInputValidator.isValidEmail(intent.email)
                it.copy(email = intent.email, showEmailError = invalid, message = null)
            }
            is LoginIntent.PasswordChange -> _state.update {
                val invalid = intent.password.isNotEmpty() && !UserInputValidator.isValidPassword(intent.password)
                it.copy(password = intent.password, showPasswordError = invalid, message = null)
            }
            LoginIntent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            LoginIntent.LoginClick -> logIn()
            LoginIntent.ForgotPasswordClick -> requestPasswordEmail()
            LoginIntent.MessageDismiss -> _state.update { it.copy(message = null) }
        }
    }

    private fun logIn() {
        val state = _state.value
        if (!state.canSubmit) return

        val emailValid = UserInputValidator.isValidEmail(state.email)
        val passwordValid = UserInputValidator.isValidPassword(state.password)
        if (!emailValid || !passwordValid) {
            _state.update { it.copy(showEmailError = !emailValid, showPasswordError = !passwordValid) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val error = login(state.email, state.password).exceptionOrNull() as? AuthError
            // On success the session changes and the app moves to the home screen by itself.
            _state.update { it.copy(isLoading = false, message = error?.let(LoginMessage::LoginFailed)) }
        }
    }

    // RF-AUTH-04: the email already typed in the form is the one that gets the link.
    private fun requestPasswordEmail() {
        val state = _state.value
        if (state.isLoading) return
        if (!UserInputValidator.isValidEmail(state.email)) {
            _state.update { it.copy(showEmailError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val sent = sendPasswordEmail(state.email).isSuccess
            val message = if (sent) LoginMessage.PasswordEmailRequested else LoginMessage.PasswordEmailFailed
            _state.update { it.copy(isLoading = false, message = message) }
        }
    }
}
