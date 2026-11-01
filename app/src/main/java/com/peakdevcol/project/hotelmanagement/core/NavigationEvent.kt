package com.peakdevcol.project.hotelmanagement.core

sealed class NavigationEvent {
    data object NavigationToBalance : NavigationEvent()
    data object NavigationToInventory : NavigationEvent()
    data object NavigationToAddOrders : NavigationEvent()
    data object NavigationToCurrentOrders : NavigationEvent()
    data object None : NavigationEvent()
}