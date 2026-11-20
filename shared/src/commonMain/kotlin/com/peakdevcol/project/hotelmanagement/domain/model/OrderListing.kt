package com.peakdevcol.project.hotelmanagement.domain.model

/** An order as the list of current orders shows it. */
data class OrderListing(
    val order: Order,
    /** Null when the profile of who took the order cannot be read. */
    val employeeName: String?,
    val isMine: Boolean,
)

enum class OrderOwnerFilter { ALL, MINE }

/** RF-CUR-02: which current orders the employee wants to see. */
data class OrderFilter(
    val owner: OrderOwnerFilter = OrderOwnerFilter.ALL,
    /** Null shows every state. */
    val state: OrderState? = null,
    /** The number of the destination or the name of who took the order. Blank finds them all. */
    val query: String = "",
) {
    fun matches(listing: OrderListing): Boolean =
        (owner == OrderOwnerFilter.ALL || listing.isMine) &&
            (state == null || listing.order.state == state) &&
            listing.matchesSearch(query)
}

/**
 * Digits look for the number of the destination, from its start; anything else looks inside the
 * name of who took the order, without minding accents or capitals.
 */
fun OrderListing.matchesSearch(query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty()) return true
    if (text.all(Char::isDigit)) return order.destination.numberOrNull?.toString()?.startsWith(text) == true
    return employeeName?.searchable()?.contains(text.searchable()) == true
}
