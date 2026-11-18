package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.fakes.burger
import com.peakdevcol.project.hotelmanagement.fakes.cashClosing
import com.peakdevcol.project.hotelmanagement.fakes.payment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class CashClosingTest {
    private val lastClosing = cashClosing(number = 4, to = "2026-11-17T02:00:00Z", createdAt = "2026-11-17T02:10:00Z")
    private val cashWithTip = payment("order_a", "2026-11-17T15:00:00Z", tipAmount = 1_000)
    private val transfer = payment("order_b", "2026-11-17T18:30:00Z", lines = listOf(burger), method = PaymentMethod.TRANSFER, tipAmount = 500)
    private val draft = CashClosingDraft(lastClosing, listOf(cashWithTip, transfer))

    @Test
    fun expectsTheCashPaymentsWithTheirTips() {
        assertEquals(3_500, draft.cashSales)
        assertEquals(1_000, draft.cashTips)
        assertEquals(4_500, draft.expectedCash)
    }

    @Test
    fun showsTheTransfersApartBecauseTheyAreNotInTheDrawer() {
        assertEquals(12_400, draft.transferTotal)
    }

    @Test
    fun startsWhereTheLastClosingEndedAndEndsWithTheLastPaymentCounted() {
        assertEquals(5, draft.number)
        assertEquals(Instant.parse("2026-11-17T02:00:00Z"), draft.from)
        assertEquals(Instant.parse("2026-11-17T18:30:00Z"), draft.to)
    }

    @Test
    fun theFirstClosingCoversEveryEarlierPayment() {
        val first = CashClosingDraft(lastClosing = null, payments = listOf(cashWithTip))

        assertEquals(1, first.number)
        assertNull(first.from)
    }

    @Test
    fun withoutPaymentsTheClosingDoesNotMoveForward() {
        val empty = CashClosingDraft(lastClosing, payments = emptyList())

        assertEquals(empty.from, empty.to)
        assertEquals(0, empty.expectedCash)
    }

    @Test
    fun theDifferenceIsCountedMinusExpected() {
        assertEquals(500, draft.differenceFor(5_000))
        assertEquals(-1_500, draft.differenceFor(3_000))
    }

    @Test
    fun aDifferenceNeedsANote() {
        assertEquals(CashClosingError.MissingNote, draft.validate(CashClosingType.DAILY, 4_000, "  "))
        assertNull(draft.validate(CashClosingType.DAILY, 4_000, "Faltó dar un vuelto"))
        assertNull(draft.validate(CashClosingType.DAILY, 4_500, ""))
    }

    @Test
    fun theTypeAndTheCountAreRequired() {
        assertEquals(CashClosingError.MissingType, draft.validate(null, 4_500, ""))
        assertEquals(CashClosingError.InvalidCount, draft.validate(CashClosingType.MANUAL, null, ""))
        assertEquals(CashClosingError.InvalidCount, draft.validate(CashClosingType.MANUAL, -1, "Nota"))
    }

    @Test
    fun aNoteHasALimit() {
        assertEquals(CashClosingError.NoteTooLong, draft.validate(CashClosingType.DAILY, 4_500, "x".repeat(501)))
    }

    @Test
    fun theClosingKeepsTheDraftValuesAndTheCleanNote() {
        val closing = draft.toClosing(CashClosingType.MANUAL, countedCash = 4_000, note = "  Vuelto  ", closedBy = "admin")

        assertEquals(5, closing.number)
        assertEquals(draft.from to draft.to, closing.from to closing.to)
        assertEquals(4_500, closing.expectedCash)
        assertEquals(-500, closing.difference)
        assertEquals("Vuelto", closing.note)
        assertNull(closing.createdAt)
    }
}
