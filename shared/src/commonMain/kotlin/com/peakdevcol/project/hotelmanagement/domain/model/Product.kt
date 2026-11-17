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
    /** RF-INV-07: the rate of the tax the price includes, in hundredths of a point. Zero when it has none. */
    val taxRate: Int = 0,
) {
    val totalAmount: Long get() = unitPrice * quantity

    /** The part of [totalAmount] that is tax. */
    val taxAmount: Long get() = taxIncludedIn(totalAmount, taxRate)
}

fun List<Product>.totalAmount(): Long = sumOf { it.totalAmount }

fun List<Product>.taxAmount(): Long = sumOf { it.taxAmount }
