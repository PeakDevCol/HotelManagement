package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * A product added to an order. Prices are in Colombian pesos, without decimals.
 */
data class Product(
    val id: String,
    val name: String,
    val unitPrice: Long,
    val quantity: Int = 1,
) {
    val totalAmount: Long get() = unitPrice * quantity
}

fun List<Product>.totalAmount(): Long = sumOf { it.totalAmount }
