package com.peakdevcol.project.hotelmanagement.domain.model

/** Where an order has to be delivered. */
sealed interface OrderDestination {
    data class Room(val number: Int) : OrderDestination
    data class Table(val number: Int) : OrderDestination
    data object Other : OrderDestination
}
