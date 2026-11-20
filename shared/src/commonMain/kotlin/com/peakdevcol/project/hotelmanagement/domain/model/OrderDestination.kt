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

/** The number of the room, table, sunbed or seat, when the destination has one. */
val OrderDestination.numberOrNull: Int?
    get() = when (this) {
        is OrderDestination.Room -> number
        is OrderDestination.Table -> number
        is OrderDestination.Pool -> number
        is OrderDestination.Bar -> number
        OrderDestination.Other -> null
    }

/** RF-ORD-03: whether a kind of destination takes a number. */
enum class DestinationNumber {
    REQUIRED,
    OPTIONAL,
    NONE,
}

val DestinationKind.number: DestinationNumber
    get() = when (this) {
        DestinationKind.ROOM, DestinationKind.TABLE -> DestinationNumber.REQUIRED
        DestinationKind.POOL, DestinationKind.BAR -> DestinationNumber.OPTIONAL
        DestinationKind.OTHER -> DestinationNumber.NONE
    }

/** A destination as it is being chosen: its kind and the number as it is typed. */
data class DestinationDraft(val kind: DestinationKind? = null, val numberText: String = "") {
    /** Null until it is complete. A number typed but not valid is never dropped silently. */
    val destination: OrderDestination?
        get() {
            val number = numberText.toIntOrNull()?.takeIf { it > 0 }
            if (numberText.isNotEmpty() && number == null && kind?.number != DestinationNumber.NONE) return null
            return when (kind) {
                DestinationKind.ROOM -> number?.let { OrderDestination.Room(it) }
                DestinationKind.TABLE -> number?.let { OrderDestination.Table(it) }
                DestinationKind.POOL -> OrderDestination.Pool(number)
                DestinationKind.BAR -> OrderDestination.Bar(number)
                DestinationKind.OTHER -> OrderDestination.Other
                null -> null
            }
        }

    companion object {
        fun of(destination: OrderDestination?): DestinationDraft =
            DestinationDraft(kind = destination?.kind(), numberText = destination?.numberOrNull?.toString().orEmpty())
    }
}
