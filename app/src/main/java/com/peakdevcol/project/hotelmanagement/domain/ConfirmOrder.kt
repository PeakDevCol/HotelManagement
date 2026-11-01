package com.peakdevcol.project.hotelmanagement.domain

import java.util.Date
import java.util.UUID

data class ConfirmOrder(
    val id: String = UUID.randomUUID().toString(),
    val idEmployee: String,
    val products: List<Product>,
    val typeOrder: ProviderTypeOrder,
    val dateOrder: Date,
    val state: OrderState,
    val totalPayAmount: Long,
) {
    companion object {
        /**
         * Builds a new order with today's date, CONFIRM state and the total calculated
         * from the products.
         */
        fun create(
            idEmployee: String,
            products: List<Product>,
            typeOrder: ProviderTypeOrder
        ): ConfirmOrder = ConfirmOrder(
            idEmployee = idEmployee,
            products = products,
            typeOrder = typeOrder,
            dateOrder = Date(),
            state = OrderState.CONFIRM,
            totalPayAmount = products.totalAmount()
        )
    }
}

fun List<Product>.totalAmount(): Long = sumOf { it.totalAmount() }
