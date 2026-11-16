package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Order(
    val id: String,
    val employeeId: String,
    val products: List<Product>,
    val destination: OrderDestination,
    val state: OrderState,
) {
    val totalAmount: Long get() = products.totalAmount()

    companion object {
        /** A new random order id. Reusing it when retrying a save overwrites the same document. */
        @OptIn(ExperimentalUuidApi::class)
        fun newId(): String = Uuid.random().toString()

        /** RF-ORD-04: a confirmed order starts in progress. The server sets its date when saved. */
        fun create(
            employeeId: String,
            products: List<Product>,
            destination: OrderDestination,
            id: String = newId(),
        ): Order = Order(
            id = id,
            employeeId = employeeId,
            products = products,
            destination = destination,
            state = OrderState.IN_PROGRESS,
        )
    }
}
