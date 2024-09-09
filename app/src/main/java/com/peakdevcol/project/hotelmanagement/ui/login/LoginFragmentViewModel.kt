package com.peakdevcol.project.hotelmanagement.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.core.Event
import com.peakdevcol.project.hotelmanagement.data.response.LoginResult
import com.peakdevcol.project.hotelmanagement.domain.LoginUseCase
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionViewState
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.MIN_PASSWORD_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginFragmentViewModel @Inject constructor(val loginUseCase: LoginUseCase) :
    ViewModel() {

    private val _navigateToHome = MutableLiveData<Event<Boolean>>()
    val navigateToHome: LiveData<Event<Boolean>>
        get() = _navigateToHome

    private val _navigateToForgotPassword = MutableLiveData<Event<Boolean>>()
    val navigateToForgotPassword: LiveData<Event<Boolean>>
        get() = _navigateToForgotPassword

    private val _navigateToVerifyAccount = MutableLiveData<Event<Boolean>>()
    val navigateToVerifyAccount: LiveData<Event<Boolean>>
        get() = _navigateToVerifyAccount

    private val _viewState = MutableStateFlow(LoginViewState())
    val viewState: StateFlow<LoginViewState>
        get() = _viewState


    fun onFieldsChanged(email: String, password: String) {
        _viewState.value =
            LoginViewState(
                isValidEmail = isValidOrEmptyEmail(email),
                isValidPassword = isValidPassword(password)
            )

    }

    /**
     * Remember that in this place arrive de parameter for the lambda
     * In this case it a parameter the provider Unit and BaseFirstStepAccountViewState
     * and if the lambda return a value for example -> (value of return)Int
     * in the last line into the lambda y need put de value of return for the example is a Int
     */
    fun onLoginSelected(
        email: String,
        password: String,
        baseStateError: () -> Unit,
        baseStateLoading: (loading: IntroductionViewState?) -> Unit
    ) {
        if (isValidOrEmptyEmail(email) && isValidPassword(password)) {
            loginUser(email, password, {
                baseStateError.invoke()
            }, {
                baseStateLoading.invoke(it)
            })
        } else {
            onFieldsChanged(email, password)
        }

    }

    /**
     *                      coroutine
     * I need to use a viewModelScope.launch because the functions that i call are suspend functions
     */
    private fun loginUser(
        email: String,
        password: String,
        baseStateError: () -> Unit,
        baseStateLoading: (loading: IntroductionViewState?) -> Unit
    ) {
        viewModelScope.launch {
            baseStateLoading.invoke(IntroductionViewState.Loading)
            when (loginUseCase(email, password, null)) {
                LoginResult.Error -> {
                    baseStateError.invoke()
                }

                is LoginResult.Success -> {
                    _navigateToHome.value = Event(true)
                }
            }
        }

    }

    private fun isValidPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH || password.isEmpty()

    private fun isValidOrEmptyEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()


}