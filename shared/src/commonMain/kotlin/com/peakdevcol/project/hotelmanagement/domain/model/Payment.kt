package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/**
 * A payment received (PRD, section 8). The balance and the cash closing are built from these,
 * so a payment is never edited.
 */
data class Payment(
    val id: String,
    /** What the order cost, taxes included. The tip goes apart (P15). */
    val amount: Long,
    val tipAmount: Long,
    /** RF-BAL-07: the part of [amount] that is tax. */
    val taxAmount: Long,
    val method: PaymentMethod,
    /** The order that was paid. */
    val sourceId: String,
    val receivedBy: String,
    val receivedAt: Instant,
    /**
     * Copied from the order when it is paid, so the balance reads only the payments. Payments
     * registered before M5 have neither a destination nor lines.
     */
    val destination: OrderDestination?,
    val lines: List<Product>,
)

/** Why the payments could not be read. */
sealed class PaymentReadError(message: String) : Exception(message) {
    data object NotAllowed : PaymentReadError("Only an administrator reads the payments")

    /** The balance needs the server: a copy of the device could leave payments out without saying so. */
    data object NoConnection : PaymentReadError("The server could not be reached")
    data object Unknown : PaymentReadError("Unknown error")
}
