package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * RF-CUR-13: products added to an order without connection, kept on the device until the server
 * answers. The server rejects them silently when the order is no longer in progress, even after
 * the app was closed and opened, so this record is the only way to tell the employee.
 */
data class PendingAddition(
    val id: String,
    val orderId: String,
    /** Kept to name the order in the warning without having to read it again. */
    val destination: OrderDestination,
    val products: List<Product>,
    /** True once the server answered without these products. It stays until the employee reads the warning. */
    val rejected: Boolean = false,
)
