package com.peakdevcol.project.hotelmanagement.data.local

import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.Product

/**
 * Writes the pending additions as text: one addition per record and one value per field.
 * Control characters separate them, and they are taken out of the values first, so a product
 * name can never break the format.
 */
internal object PendingAdditionCodec {
    private const val RECORD = '\u001E'
    private const val FIELD = '\u001F'
    private const val PRODUCT = '\u001D'
    private const val PRODUCT_FIELD = '\u001C'
    private const val NO_NUMBER = ""
    private val SEPARATORS = setOf(RECORD, FIELD, PRODUCT, PRODUCT_FIELD)

    fun encode(additions: List<PendingAddition>): String = additions.joinToString(RECORD.toString()) { addition ->
        listOf(
            addition.id.clean(),
            addition.orderId.clean(),
            addition.destination.typeCode(),
            addition.destination.number()?.toString() ?: NO_NUMBER,
            addition.rejected.toString(),
            addition.products.joinToString(PRODUCT.toString()) { it.encode() },
        ).joinToString(FIELD.toString())
    }

    /** A record that cannot be read is skipped: a damaged file must not stop the app. */
    fun decode(text: String): List<PendingAddition> =
        text.split(RECORD).filter { it.isNotEmpty() }.mapNotNull { it.toAddition() }

    private fun String.toAddition(): PendingAddition? {
        val fields = split(FIELD)
        if (fields.size != 6) return null
        val destination = destinationOf(fields[2], fields[3].toIntOrNull()) ?: return null
        val products = fields[5].split(PRODUCT).filter { it.isNotEmpty() }.map { it.toProduct() ?: return null }
        return PendingAddition(
            id = fields[0],
            orderId = fields[1],
            destination = destination,
            products = products,
            rejected = fields[4].toBoolean(),
        )
    }

    private fun Product.encode(): String =
        listOf(lineId.orEmpty(), id, name, unitPrice.toString(), quantity.toString())
            .joinToString(PRODUCT_FIELD.toString()) { it.clean() }

    private fun String.toProduct(): Product? {
        val fields = split(PRODUCT_FIELD)
        if (fields.size != 5) return null
        return Product(
            id = fields[1],
            name = fields[2],
            unitPrice = fields[3].toLongOrNull() ?: return null,
            quantity = fields[4].toIntOrNull() ?: return null,
            lineId = fields[0].ifEmpty { null },
        )
    }

    private fun String.clean(): String = filterNot { it in SEPARATORS }

    private fun OrderDestination.typeCode(): String = when (this) {
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

    private fun destinationOf(type: String, number: Int?): OrderDestination? = when (type) {
        "room" -> number?.let { OrderDestination.Room(it) }
        "table" -> number?.let { OrderDestination.Table(it) }
        "pool" -> OrderDestination.Pool(number)
        "bar" -> OrderDestination.Bar(number)
        "other" -> OrderDestination.Other
        else -> null
    }
}
