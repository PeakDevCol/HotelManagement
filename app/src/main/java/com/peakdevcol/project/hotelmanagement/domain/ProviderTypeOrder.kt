package com.peakdevcol.project.hotelmanagement.domain

sealed class ProviderTypeOrder {
    data class Room(val roomNumber: Int) : ProviderTypeOrder()
    data class Table(val tableNumber: Int) : ProviderTypeOrder()
    data object Other : ProviderTypeOrder()
}