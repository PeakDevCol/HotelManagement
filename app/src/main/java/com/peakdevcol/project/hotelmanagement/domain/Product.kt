package com.peakdevcol.project.hotelmanagement.domain

data class Product(
    val productId: String,
    val productName: String,
    val productQuantity: String,
    val productAmount: String,
) {
    fun setQuantityProduct(quantity: String): Product {
        return this.copy(productQuantity = quantity)
    }

    /** Unit price multiplied by quantity. Invalid numbers count as 0. */
    fun totalAmount(): Long =
        (productQuantity.toLongOrNull() ?: 0L) * (productAmount.toLongOrNull() ?: 0L)

}
