package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** A payment received, stored in `Payments`. The balance and the cash closing are built from these. */
@Serializable
internal data class PaymentDto(
    val amount: Long,
    val tipAmount: Long,
    val method: String,
    val source: String,
    val sourceId: String,
    val receivedBy: String,
    val receivedAt: BaseTimestamp = Timestamp.ServerTimestamp,
)

internal const val PAYMENT_SOURCE_ORDER = "order"

/** P15: the amount is what the order costs, and the tip goes apart. */
internal fun OrderStateChange.Pay.toPaymentDto(orderId: String, orderAmount: Long) = PaymentDto(
    amount = orderAmount,
    tipAmount = tipAmount,
    method = method.code(),
    source = PAYMENT_SOURCE_ORDER,
    sourceId = orderId,
    receivedBy = employeeId,
)

// RF-CUR-08: one fixed id per order. The rules only let a payment be created, so an order cannot be paid twice.
internal fun orderPaymentId(orderId: String) = "order_$orderId"
