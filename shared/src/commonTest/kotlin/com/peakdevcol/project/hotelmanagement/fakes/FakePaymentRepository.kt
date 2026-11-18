package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Payment
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.PaymentRepository
import kotlin.time.Instant

/** Follows the contract of Firestore: from included, until excluded. */
class FakePaymentRepository(vararg payments: Payment) : PaymentRepository {
    var payments: List<Payment> = payments.toList()
    var failure: PaymentReadError? = null

    /** Every range that was read, so tests can count the reads. */
    val reads = mutableListOf<Pair<Instant, Instant>>()

    override suspend fun paymentsReceived(from: Instant, until: Instant): Result<List<Payment>> {
        failure?.let { return Result.failure(it) }
        reads += from to until
        return Result.success(payments.filter { it.receivedAt >= from && it.receivedAt < until })
    }

    /** The "after" of every read since a closing. */
    val readsAfter = mutableListOf<Instant?>()

    override suspend fun paymentsReceivedAfter(after: Instant?): Result<List<Payment>> {
        failure?.let { return Result.failure(it) }
        readsAfter += after
        return Result.success(payments.filter { after == null || it.receivedAt > after })
    }
}

val beer = Product(id = "beer", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 1, lineId = "line-beer")

/** 19 % included: $11.900 has $1.900 of tax. */
val burger = Product(id = "burger", name = "Hamburguesa", unitPrice = 11_900, quantity = 1, lineId = "line-burger", taxRate = 1_900)

fun payment(
    id: String,
    receivedAt: String,
    lines: List<Product> = listOf(beer),
    tipAmount: Long = 0,
    method: PaymentMethod = PaymentMethod.CASH,
    receivedBy: String = "uid-1",
    destination: OrderDestination? = OrderDestination.Table(3),
) = Payment(
    id = id,
    amount = lines.sumOf { it.totalAmount },
    tipAmount = tipAmount,
    taxAmount = lines.sumOf { it.taxAmount },
    method = method,
    sourceId = id.removePrefix("order_"),
    receivedBy = receivedBy,
    receivedAt = Instant.parse(receivedAt),
    destination = destination,
    lines = lines,
)
