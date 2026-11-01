package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import kotlinx.coroutines.launch

class HomeViewModel(private val logOut: LogOutUseCase) : ViewModel() {

    /** The session change takes the app back to the introduction screen. */
    fun onLogOutConfirmed() {
        viewModelScope.launch { logOut() }
    }
}
