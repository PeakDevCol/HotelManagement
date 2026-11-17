package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoginUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.ui.login.LoginIntent
import com.peakdevcol.project.hotelmanagement.ui.login.LoginMessage
import com.peakdevcol.project.hotelmanagement.ui.login.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest : MainDispatcherTest() {

    private val repository = FakeAuthRepository()
    private val viewModel = LoginViewModel(LoginUseCase(repository), SendPasswordEmailUseCase(repository))

    private fun type(email: String, password: String = "") {
        viewModel.onIntent(LoginIntent.EmailChange(email))
        viewModel.onIntent(LoginIntent.PasswordChange(password))
    }

    @Test
    fun showsFieldErrorsWhileTypingInvalidValues() {
        type("juan@", "123")

        val state = viewModel.state.value
        assertTrue(state.showEmailError)
        assertTrue(state.showPasswordError)
    }

    @Test
    fun emptyFieldsAreNotErrorsButCannotBeSubmitted() {
        type("")

        val state = viewModel.state.value
        assertFalse(state.showEmailError)
        assertFalse(state.canSubmit)
    }

    @Test
    fun doesNotCallFirebaseWithInvalidData() {
        type("juan", "123456")

        viewModel.onIntent(LoginIntent.LoginClick)

        assertTrue(repository.loginCalls.isEmpty())
        assertTrue(viewModel.state.value.showEmailError)
    }

    @Test
    fun logsInWithTrimmedEmail() {
        type(" juan@hotel.com ", "123456")

        viewModel.onIntent(LoginIntent.LoginClick)

        assertEquals("juan@hotel.com" to "123456", repository.loginCalls.single())
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun showsErrorWhenCredentialsAreWrong() {
        repository.loginResult = Result.failure(AuthError.InvalidCredentials)
        type("juan@hotel.com", "123456")

        viewModel.onIntent(LoginIntent.LoginClick)
        assertEquals(LoginMessage.LoginFailed(AuthError.InvalidCredentials), viewModel.state.value.message)

        viewModel.onIntent(LoginIntent.MessageDismiss)
        assertNull(viewModel.state.value.message)
    }

    // ---- RF-AUTH-04

    @Test
    fun forgotPasswordSendsTheEmailToTheAddressInTheForm() {
        type(" Juan@Hotel.com ")

        viewModel.onIntent(LoginIntent.ForgotPasswordClick)

        assertEquals(listOf("juan@hotel.com"), repository.passwordEmails)
        assertEquals(LoginMessage.PasswordEmailRequested, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun forgotPasswordNeedsAValidEmailFirst() {
        type("juan@")

        viewModel.onIntent(LoginIntent.ForgotPasswordClick)

        assertTrue(repository.passwordEmails.isEmpty())
        assertTrue(viewModel.state.value.showEmailError)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun tellsWhenThePasswordEmailCouldNotBeRequested() {
        repository.passwordEmailResult = Result.failure(AuthError.Network)
        type("juan@hotel.com")

        viewModel.onIntent(LoginIntent.ForgotPasswordClick)

        assertEquals(LoginMessage.PasswordEmailFailed, viewModel.state.value.message)
    }
}
