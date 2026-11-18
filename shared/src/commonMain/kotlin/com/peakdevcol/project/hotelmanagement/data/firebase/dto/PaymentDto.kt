package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.Payment
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A payment received, stored in `Payments`. The balance and the cash closing are built from these.
 * Since M5 it carries a copy of the destination, the tax and the lines of the order, so the
 * balance reads one document per sale (decision of 2026-11-17). The rules check the copy.
 */
@Serializable
internal data class PaymentDto(
    val amount: Long,
    val tipAmount: Long,
    val method: String,
    val source: String,
    val sourceId: String,
    val receivedBy: String,
    val receivedAt: BaseTimestamp = Timestamp.ServerTimestamp,
    val taxAmount: Long = 0,
    val typeOrder: String? = null,
    val typeOrderNumber: Int? = null,
    val products: List<ProductDto> = emptyList(),
)

internal const val PAYMENT_SOURCE_ORDER = "order"

/**
 * P15: the amount is what the order costs, and the tip goes apart. The lines, the destination and
 * the tax are copied exactly as the server has them, because the rules compare them with the order.
 */
internal fun OrderStateChange.Pay.toPaymentDto(orderId: String, order: OrderDto, orderAmount: Long) = PaymentDto(
    amount = orderAmount,
    tipAmount = tipAmount,
    method = method.code(),
    source = PAYMENT_SOURCE_ORDER,
    sourceId = orderId,
    receivedBy = employeeId,
    taxAmount = order.taxAmount,
    typeOrder = order.typeOrder,
    typeOrderNumber = order.typeOrderNumber,
    products = order.products,
)

/** Returns null when the method is not one the app knows or the server date has not arrived. */
internal fun PaymentDto.toPayment(id: String): Payment? {
    val paymentMethod = PaymentMethod.entries.firstOrNull { it.code() == method } ?: return null
    val receivedAtInstant = (receivedAt as? Timestamp)?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) } ?: return null
    return Payment(
        id = id,
        amount = amount,
        tipAmount = tipAmount,
        taxAmount = taxAmount,
        method = paymentMethod,
        sourceId = sourceId,
        receivedBy = receivedBy,
        receivedAt = receivedAtInstant,
        destination = typeOrder?.let { destinationOf(it, typeOrderNumber) },
        lines = products.toProducts(),
    )
}

// RF-CUR-08: one fixed id per order. The rules only let a payment be created, so an order cannot be paid twice.
internal fun orderPaymentId(orderId: String) = "order_$orderId"
