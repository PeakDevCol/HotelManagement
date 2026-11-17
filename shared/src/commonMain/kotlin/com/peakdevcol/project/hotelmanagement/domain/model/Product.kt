package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * A product added to an order. Prices are in Colombian pesos, without decimals.
 */
data class Product(
    val id: String,
    val name: String,
    val unitPrice: Long,
    val quantity: Int = 1,
    /**
     * Identifies this line inside an order, where the same product can be added more than once.
     * A product of the catalog has none.
     */
    val lineId: String? = null,
) {
    val totalAmount: Long get() = unitPrice * quantity
}

fun List<Product>.totalAmount(): Long = sumOf { it.totalAmount }
