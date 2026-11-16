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
) {
    fun matches(listing: OrderListing): Boolean =
        (owner == OrderOwnerFilter.ALL || listing.isMine) && (state == null || listing.order.state == state)
}
