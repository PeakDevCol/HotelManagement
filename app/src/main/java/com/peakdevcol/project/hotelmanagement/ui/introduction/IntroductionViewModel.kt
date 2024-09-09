package com.peakdevcol.project.hotelmanagement.ui.introduction

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class IntroductionViewModel @Inject constructor() : ViewModel() {


    private val _viewState = MutableStateFlow<IntroductionViewState?>(null)
    val viewState: StateFlow<IntroductionViewState?>
        get() = _viewState

    fun setViewState(loading: IntroductionViewState?) {
        _viewState.value = loading
    }

}