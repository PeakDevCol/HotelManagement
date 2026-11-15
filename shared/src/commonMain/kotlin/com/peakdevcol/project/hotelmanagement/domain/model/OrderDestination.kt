package com.peakdevcol.project.hotelmanagement.domain.model

/** Where an order has to be delivered. */
sealed interface OrderDestination {
    data class Room(val number: Int) : OrderDestination
    data class Table(val number: Int) : OrderDestination

    /** The number is the sunbed, when the waiter wants to note it. */
    data class Pool(val number: Int? = null) : OrderDestination

    /** The number is the seat at the bar, when the waiter wants to note it. */
    data class Bar(val number: Int? = null) : OrderDestination
    data object Other : OrderDestination
}
