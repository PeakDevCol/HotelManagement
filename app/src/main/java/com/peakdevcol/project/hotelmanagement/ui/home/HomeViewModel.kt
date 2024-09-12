package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(private val homeRepository: HomeRepository) : ViewModel() {


    private val _viewState = MutableStateFlow<HomeViewState>(HomeViewState.Loading)
    val viewState: StateFlow<HomeViewState> = _viewState

    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.logOut()
        }
    }

    fun navigate(navigate: HomeViewState.Navigate) {
        _viewState.value = navigate
    }

    fun setTopAppBarTitle(title: String) {
        _viewState.value = HomeViewState.TopBarTitle(titleTopBar = title)
    }

}