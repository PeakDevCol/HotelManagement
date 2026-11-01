package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.ui.login.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest : MainDispatcherTest() {

    private val repository = FakeAuthRepository()
    private val viewModel = LoginViewModel(LoginUseCase(repository))

    @Test
    fun showsFieldErrorsWhileTypingInvalidValues() {
        viewModel.onEmailChange("juan@")
        viewModel.onPasswordChange("123")

        val state = viewModel.uiState.value
        assertTrue(state.showEmailError)
        assertTrue(state.showPasswordError)
    }

    @Test
    fun emptyFieldsAreNotErrorsButCannotBeSubmitted() {
        viewModel.onEmailChange("")

        val state = viewModel.uiState.value
        assertFalse(state.showEmailError)
        assertFalse(state.canSubmit)
    }

    @Test
    fun doesNotCallFirebaseWithInvalidData() {
        viewModel.onEmailChange("juan")
        viewModel.onPasswordChange("123456")

        viewModel.onLoginClick()

        assertTrue(repository.loginCalls.isEmpty())
        assertTrue(viewModel.uiState.value.showEmailError)
    }

    @Test
    fun logsInWithTrimmedEmail() {
        viewModel.onEmailChange(" juan@hotel.com ")
        viewModel.onPasswordChange("123456")

        viewModel.onLoginClick()

        assertEquals("juan@hotel.com" to "123456", repository.loginCalls.single())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun showsErrorWhenCredentialsAreWrong() {
        repository.loginResult = Result.failure(AuthError.InvalidCredentials)
        viewModel.onEmailChange("juan@hotel.com")
        viewModel.onPasswordChange("123456")

        viewModel.onLoginClick()
        assertEquals(AuthError.InvalidCredentials, viewModel.uiState.value.error)

        viewModel.onErrorDismissed()
        assertNull(viewModel.uiState.value.error)
    }
}
