package com.peakdevcol.project.hotelmanagement.domain.model

/** RF-ORD-01: a product goes in an order with 1 to 99 units. */
const val MAX_PRODUCT_QUANTITY = 99

/**
 * The products chosen for a new order or to add to one, in the order they were first chosen.
 * Choosing a product that is already there adds one unit (RF-ORD-01).
 */
data class ProductSelection(val products: List<Product> = emptyList()) {
    val totalAmount: Long get() = products.totalAmount()
    val unitCount: Int get() = products.sumOf { it.quantity }
    val isEmpty: Boolean get() = products.isEmpty()

    fun quantityOf(productId: String): Int = products.firstOrNull { it.id == productId }?.quantity ?: 0

    fun add(product: Product): ProductSelection =
        if (quantityOf(product.id) > 0) increase(product.id) else copy(products = products + product.copy(quantity = 1, lineId = null))

    fun increase(productId: String): ProductSelection =
        changeQuantity(productId) { (it + 1).coerceAtMost(MAX_PRODUCT_QUANTITY) }

    /** One unit less; the last unit stays, because taking the product out is [remove]. */
    fun decrease(productId: String): ProductSelection = changeQuantity(productId) { (it - 1).coerceAtLeast(1) }

    fun remove(productId: String): ProductSelection = copy(products = products.filterNot { it.id == productId })

    private fun changeQuantity(productId: String, change: (Int) -> Int): ProductSelection =
        copy(products = products.map { if (it.id == productId) it.copy(quantity = change(it.quantity)) else it })
}
