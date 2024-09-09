package com.peakdevcol.project.hotelmanagement.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.peakdevcol.project.hotelmanagement.core.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FirstScreenViewModel @Inject constructor(
    private val authFireBase: FirebaseAuth,
) : ViewModel() {

    private val _authUser = MutableLiveData<Event<Boolean>>()
    val authUser: LiveData<Event<Boolean>>
        get() = _authUser

    fun checkAuthUser() {
        _authUser.value = Event(content = (authFireBase.currentUser != null))
    }

}