package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Order(
    val id: String,
    val employeeId: String,
    val products: List<Product>,
    val destination: OrderDestination,
    val state: OrderState,
    /** Set by the server. Until the order reaches it, this is the estimate of the device. */
    val createdAt: Instant? = null,
    /** RF-ORD-09: saved on this device, but the server does not have it yet. */
    val isPendingToSend: Boolean = false,
    /** False while the device shows its own copy because it has not heard from the server. */
    val isUpToDate: Boolean = true,
) {
    val totalAmount: Long get() = products.totalAmount()

    companion object {
        /** A new random id, for an order or for one of its lines. Reusing the id of an order when retrying a save overwrites the same document. */
        @OptIn(ExperimentalUuidApi::class)
        fun newId(): String = Uuid.random().toString()

        /** RF-CUR-13: every line gets its own id, so two devices can add lines to the same order without replacing each other's. */
        fun linesOf(products: List<Product>): List<Product> =
            products.map { it.copy(lineId = it.lineId ?: newId()) }

        /** RF-ORD-04: a confirmed order starts in progress. The server sets its date when saved. */
        fun create(
            employeeId: String,
            products: List<Product>,
            destination: OrderDestination,
            id: String = newId(),
        ): Order = Order(
            id = id,
            employeeId = employeeId,
            products = linesOf(products),
            destination = destination,
            state = OrderState.IN_PROGRESS,
        )
    }
}
