package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

enum class OrderEventType {
    CREATED,
    CLOSED,
    REOPENED,
    PAID,
    CANCELLED,
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
)

/** RF-CUR-03: everything the detail of an order shows. */
data class OrderDetail(
    val order: Order,
    val employeeName: String?,
    val history: List<OrderEvent>,
    /** What the logged-in employee may do with the order, by its state and their role. */
    val availableActions: List<OrderAction>,
)
