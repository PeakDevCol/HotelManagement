package com.peakdevcol.project.hotelmanagement.ui.signin

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.core.Event
import com.peakdevcol.project.hotelmanagement.domain.CreateAccountUseCase
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionViewState
import com.peakdevcol.project.hotelmanagement.ui.signin.model.FullUserSignIn
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.MIN_PASSWORD_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(val createAccountUseCase: CreateAccountUseCase) :
    ViewModel() {


    private val _viewState = MutableStateFlow(SignInViewState())
    val viewState: StateFlow<SignInViewState>
        get() = _viewState

    private val _navigateToHome = MutableLiveData<Event<Boolean>>()
    val navigateToHome: LiveData<Event<Boolean>>
        get() = _navigateToHome


    fun onFieldsChanged(userSignIn: FullUserSignIn) {
        _viewState.value = userSignIn.toSignInViewState()
    }

    private fun signInUser(
        userSignIn: FullUserSignIn,
        baseStateError: () -> Unit,
        baseStateLoading: (loading: IntroductionViewState?) -> Unit
    ) {
        viewModelScope.launch {
            baseStateLoading.invoke(IntroductionViewState.Loading)
            val accountCreated = createAccountUseCase(userSignIn)
            if (accountCreated) {
                _navigateToHome.value = Event(true)
                baseStateLoading.invoke(null)
            } else {
                baseStateError.invoke()
            }
        }
    }

    private fun FullUserSignIn.toSignInViewState(): SignInViewState {
        return SignInViewState(
            isValidFullName = isValidOrEmptyName(fullName),
            isValidEmail = isValidOrEmptyEmail(email),
            isValidPassword = isValidAndSamePassword(password, passwordConfirmation)
        )
    }

    private fun isValidAndSamePassword(password: String, passwordConfirmation: String): Boolean =
        password.isEmpty() || passwordConfirmation.isEmpty() || (password.length >= MIN_PASSWORD_LENGTH && password == passwordConfirmation)


    private fun isValidOrEmptyName(fullName: String): Boolean =
        fullName.isEmpty() || fullName.length >= MIN_PASSWORD_LENGTH

    private fun isValidOrEmptyEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()

    fun onSignInSelected(
        userSignIn: FullUserSignIn,
        baseStateError: () -> Unit,
        baseStateLoading: (loading: IntroductionViewState?) -> Unit
    ) {
        val viewState = userSignIn.toSignInViewState()
        if (viewState.userValidated() && userSignIn.isNotEmpty()) {
            signInUser(userSignIn, {
                baseStateError.invoke()
            }, {
                baseStateLoading.invoke(it)
            })
        } else {
            onFieldsChanged(userSignIn)
        }
    }


}



