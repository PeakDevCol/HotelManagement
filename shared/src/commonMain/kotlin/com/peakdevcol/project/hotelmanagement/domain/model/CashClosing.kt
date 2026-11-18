package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/** RF-BAL-06: "Cierre del día" or "Cierre de turno", chosen by the administrator when saving. */
enum class CashClosingType {
    DAILY,
    MANUAL,
}

/**
 * A cash closing saved in `CashClosings`. It is never edited. Closings are numbered 1, 2, 3… and
 * each one covers the payments received after the [to] of the one before, up to its own [to].
 */
data class CashClosing(
    val number: Long,
    /** The [to] of the closing before. Null for the first closing, which covers every earlier payment. */
    val from: Instant?,
    /**
     * When the last payment it counted was received, or its [from] when it counted none. A payment
     * registered while the cash was being counted goes to the next closing.
     */
    val to: Instant?,
    val type: CashClosingType,
    val expectedCash: Long,
    val countedCash: Long,
    /** Counted minus expected: positive when there is more cash than expected. */
    val difference: Long,
    val note: String,
    val closedBy: String,
    /** Set by the server when it is saved. */
    val createdAt: Instant? = null,
)

/**
 * What the next closing would cover: the payments received since the last closing. It only
 * counts cash; transfers are shown apart because they are not in the cash drawer.
 */
data class CashClosingDraft(
    val lastClosing: CashClosing?,
    val payments: List<Payment>,
) {
    val number: Long get() = (lastClosing?.number ?: 0) + 1
    val from: Instant? get() = lastClosing?.to
    val to: Instant? get() = payments.maxOfOrNull { it.receivedAt } ?: from

    private val cashPayments get() = payments.filter { it.method == PaymentMethod.CASH }
    private val transferPayments get() = payments.filter { it.method == PaymentMethod.TRANSFER }

    val cashSales: Long get() = cashPayments.sumOf { it.amount }
    val cashTips: Long get() = cashPayments.sumOf { it.tipAmount }

    /** RF-BAL-06: the cash payments with their tips. */
    val expectedCash: Long get() = cashSales + cashTips
    val transferTotal: Long get() = transferPayments.sumOf { it.amount + it.tipAmount }

    fun differenceFor(countedCash: Long): Long = countedCash - expectedCash

    /** The closing to save. Call it only after [validate] found nothing missing. */
    fun toClosing(type: CashClosingType, countedCash: Long, note: String, closedBy: String) = CashClosing(
        number = number,
        from = from,
        to = to,
        type = type,
        expectedCash = expectedCash,
        countedCash = countedCash,
        difference = differenceFor(countedCash),
        note = note.trim(),
        closedBy = closedBy,
    )

    /** The input of the administrator is complete, or why it is not. */
    fun validate(type: CashClosingType?, countedCash: Long?, note: String): CashClosingError? = when {
        type == null -> CashClosingError.MissingType
        countedCash == null || countedCash < 0 -> CashClosingError.InvalidCount
        // O3: the difference is $0 or the note justifies it.
        differenceFor(countedCash) != 0L && note.isBlank() -> CashClosingError.MissingNote
        note.trim().length > MAX_NOTE_LENGTH -> CashClosingError.NoteTooLong
        else -> null
    }

    companion object {
        const val MAX_NOTE_LENGTH = 500
    }
}

/** What the cash closing screen shows: the next closing and the latest ones saved. */
data class CashClosingOverview(
    val draft: CashClosingDraft,
    /** The latest first. */
    val recentClosings: List<CashClosing>,
)

sealed class CashClosingError(message: String) : Exception(message) {
    data object MissingType : CashClosingError("Choose a daily or a shift closing")
    data object InvalidCount : CashClosingError("The counted cash is not a valid amount")
    data object MissingNote : CashClosingError("A difference needs a note")
    data object NoteTooLong : CashClosingError("The note is too long")
    data object NoSession : CashClosingError("There is no logged-in employee")
    data object NotAllowed : CashClosingError("Only an administrator closes the cash")

    /** Another administrator saved a closing first: this one would cover the same payments. */
    data object AlreadyClosed : CashClosingError("Another closing was saved first")
    data object NoConnection : CashClosingError("The server could not be reached")
    data object Unknown : CashClosingError("Unknown error")
}
