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
        /** Builds a new confirmed order. The creation date is set by the server when saved. */
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            employeeId: String,
            products: List<Product>,
            destination: OrderDestination,
            id: String = Uuid.random().toString(),
        ): Order = Order(
            id = id,
            employeeId = employeeId,
            products = products,
            destination = destination,
            state = OrderState.CONFIRM,
        )
    }
}
