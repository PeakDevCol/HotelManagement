package com.peakdevcol.project.hotelmanagement.ui.home

import com.peakdevcol.project.hotelmanagement.core.NavigationEvent

sealed class HomeViewState(val navigationEvent: NavigationEvent = NavigationEvent.None) {
    data object Loading : HomeViewState()
    data object Error : HomeViewState()
    data class Navigate(val destination: NavigationEvent) : HomeViewState(destination)
}
