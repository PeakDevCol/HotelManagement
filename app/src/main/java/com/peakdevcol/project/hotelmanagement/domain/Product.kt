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

}