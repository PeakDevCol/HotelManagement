package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class OrderDto(
    val id: String,
    val idEmployee: String,
    val products: List<ProductDto>,
    val typeOrder: String,
    val typeOrderNumber: Int?,
    val dateOrder: BaseTimestamp = Timestamp.ServerTimestamp,
    val state: String,
    val totalPayAmount: Long,
)

@Serializable
internal data class ProductDto(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitAmount: Long,
    val totalAmount: Long,
)

/** Returns null when the state or the destination is not one of section 8 of the PRD. */
internal fun OrderDto.toOrder(isPendingToSend: Boolean, isUpToDate: Boolean = true): Order? {
    val orderState = OrderState.entries.firstOrNull { it.code() == state } ?: return null
    val destination = destinationOf(typeOrder, typeOrderNumber) ?: return null
    return Order(
        id = id,
        employeeId = idEmployee,
        products = products.map { it.toProduct() },
        destination = destination,
        state = orderState,
        createdAt = (dateOrder as? Timestamp)?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) },
        isPendingToSend = isPendingToSend,
        isUpToDate = isUpToDate,
    )
}

private fun ProductDto.toProduct() = Product(id = productId, name = productName, unitPrice = unitAmount, quantity = quantity)

private fun destinationOf(type: String, number: Int?): OrderDestination? = when (type) {
    "room" -> number?.let { OrderDestination.Room(it) }
    "table" -> number?.let { OrderDestination.Table(it) }
    "pool" -> OrderDestination.Pool(number)
    "bar" -> OrderDestination.Bar(number)
    "other" -> OrderDestination.Other
    else -> null
}

internal fun Order.toDto() = OrderDto(
    id = id,
    idEmployee = employeeId,
    products = products.map { it.toDto() },
    typeOrder = destination.typeName(),
    typeOrderNumber = destination.number(),
    state = state.code(),
    totalPayAmount = totalAmount,
)

private fun Product.toDto() = ProductDto(
    productId = id,
    productName = name,
    quantity = quantity,
    unitAmount = unitPrice,
    totalAmount = totalAmount,
)

/** The state codes of section 8 of the PRD, which the Firestore rules also check. */
internal fun OrderState.code(): String = name.lowercase()

private fun OrderDestination.typeName(): String = when (this) {
    is OrderDestination.Room -> "room"
    is OrderDestination.Table -> "table"
    is OrderDestination.Pool -> "pool"
    is OrderDestination.Bar -> "bar"
    OrderDestination.Other -> "other"
}

private fun OrderDestination.number(): Int? = when (this) {
    is OrderDestination.Room -> number
    is OrderDestination.Table -> number
    is OrderDestination.Pool -> number
    is OrderDestination.Bar -> number
    OrderDestination.Other -> null
}
