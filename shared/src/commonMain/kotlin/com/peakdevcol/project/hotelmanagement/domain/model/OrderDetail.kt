package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

enum class OrderEventType {
    CREATED,
    CLOSED,
    REOPENED,
    PAID,
    CANCELLED,
    PRODUCTS_ADDED,
    PRODUCTS_REMOVED,
}

/** RF-CUR-11: one line of the history of an order. */
data class OrderEvent(
    val type: OrderEventType,
    val employeeId: String,
    /** Null when the profile of the employee cannot be read. */
    val employeeName: String? = null,
    /** Null only for a moment, while the server has not dated the event yet. */
    val at: Instant?,
    val cancelReason: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val tipAmount: Long? = null,
    /** The products that were added or removed. */
    val products: List<Product> = emptyList(),
)

/** RF-CUR-03: everything the detail of an order shows. */
data class OrderDetail(
    val order: Order,
    val employeeName: String?,
    val history: List<OrderEvent>,
    /** What the logged-in employee may do with the order, by its state and their role. */
    val availableActions: List<OrderAction>,
) {
    /** RF-CUR-06: products are added and removed only while the order is in progress. */
    val canEditProducts: Boolean get() = order.state == OrderState.IN_PROGRESS && availableActions.isNotEmpty()

    /** Removing needs the server, and an order never stays without products. */
    val canRemoveProducts: Boolean get() = canEditProducts && order.isUpToDate && order.products.size > 1
}
