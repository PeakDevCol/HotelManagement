package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateAccountUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.ui.signup.SignUpViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignUpViewModelTest : MainDispatcherTest() {

    private val repository = FakeAuthRepository()
    private val viewModel = SignUpViewModel(CreateAccountUseCase(repository))

    private fun fillForm(confirmPassword: String = "secreto1") {
        viewModel.onFullNameChange("Juan Garcia")
        viewModel.onEmailChange("juan@hotel.com")
        viewModel.onPasswordChange("secreto1")
        viewModel.onConfirmPasswordChange(confirmPassword)
    }

    @Test
    fun showsErrorWhenPasswordsDoNotMatch() {
        fillForm(confirmPassword = "otra-clave")

        viewModel.onCreateAccountClick()

        assertTrue(viewModel.uiState.value.showConfirmPasswordError)
        assertTrue(repository.createdUsers.isEmpty())
    }

    @Test
    fun createsAccountWithoutSendingThePasswordAsProfileData() {
        fillForm()

        viewModel.onCreateAccountClick()

        val (user, password) = repository.createdUsers.single()
        assertEquals(NewUser(fullName = "Juan Garcia", email = "juan@hotel.com"), user)
        assertEquals("secreto1", password)
    }

    @Test
    fun showsErrorWhenEmailIsAlreadyRegistered() {
        repository.createAccountResult = Result.failure(AuthError.EmailAlreadyInUse)
        fillForm()

        viewModel.onCreateAccountClick()

        assertEquals(AuthError.EmailAlreadyInUse, viewModel.uiState.value.error)
    }
}
