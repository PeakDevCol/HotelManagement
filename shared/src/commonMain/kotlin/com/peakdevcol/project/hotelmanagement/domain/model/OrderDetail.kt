package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

enum class OrderEventType { CREATED }

/** RF-CUR-11: one line of the history of an order. */
data class OrderEvent(
    val type: OrderEventType,
    val employeeName: String?,
    val at: Instant?,
)

/** RF-CUR-03: everything the detail of an order shows. */
data class OrderDetail(
    val order: Order,
    val employeeName: String?,
    val history: List<OrderEvent>,
)
