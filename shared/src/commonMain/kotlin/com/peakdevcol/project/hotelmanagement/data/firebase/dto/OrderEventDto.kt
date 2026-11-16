package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** A change stored at `Orders/{id}/events/{id}` (RF-CUR-11). It is created once and never edited. */
@Serializable
internal data class OrderEventDto(
    val type: String,
    val by: String,
    val at: BaseTimestamp = Timestamp.ServerTimestamp,
    val reason: String? = null,
    val method: String? = null,
    val tipAmount: Long? = null,
    val products: List<ProductDto>? = null,
)

internal fun productsEventDto(type: OrderEventType, employeeId: String, products: List<Product>) =
    OrderEventDto(type = type.code(), by = employeeId, products = products.map { it.toDto() })

internal fun OrderStateChange.toEventDto() = when (this) {
    is OrderStateChange.Close -> OrderEventDto(type = OrderEventType.CLOSED.code(), by = employeeId)
    is OrderStateChange.Reopen -> OrderEventDto(type = OrderEventType.REOPENED.code(), by = employeeId)
    is OrderStateChange.Pay -> OrderEventDto(
        type = OrderEventType.PAID.code(),
        by = employeeId,
        method = method.code(),
        tipAmount = tipAmount,
    )
    is OrderStateChange.Cancel -> OrderEventDto(type = OrderEventType.CANCELLED.code(), by = employeeId, reason = reason)
}

/** Returns null when the type is not one the app knows. */
internal fun OrderEventDto.toEvent(): OrderEvent? {
    val eventType = OrderEventType.entries.firstOrNull { it.code() == type } ?: return null
    return OrderEvent(
        type = eventType,
        employeeId = by,
        at = (at as? Timestamp)?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) },
        cancelReason = reason,
        paymentMethod = PaymentMethod.entries.firstOrNull { it.code() == method },
        tipAmount = tipAmount,
        products = products?.toProducts().orEmpty(),
    )
}

internal fun OrderEventType.code(): String = name.lowercase()

internal fun PaymentMethod.code(): String = name.lowercase()
