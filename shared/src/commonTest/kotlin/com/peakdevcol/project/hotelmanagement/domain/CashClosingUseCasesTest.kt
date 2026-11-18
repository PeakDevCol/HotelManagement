package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeCashClosingRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePaymentRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.cashClosing
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.payment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CashClosingUseCasesTest {
    private val payments = FakePaymentRepository(
        payment("order_before", "2026-11-16T20:00:00Z"),
        payment("order_after", "2026-11-17T20:00:00Z", tipAmount = 500),
    )
    private val closings = FakeCashClosingRepository()
    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(employee("admin", "Juan Felipe", type = UserType.ADMIN), employee("mesero", "Ana"))

    private fun loadCashClosing() = LoadCashClosingUseCase(closings, payments)
    private fun saveCashClosing() = SaveCashClosingUseCase(auth, users, closings)

    @Test
    fun theFirstClosingCountsEveryPayment() = runTest {
        val draft = loadCashClosing()().getOrThrow().draft

        assertEquals(1, draft.number)
        assertEquals(listOf<Instant?>(null), payments.readsAfter)
        assertEquals(2, draft.payments.size)
    }

    @Test
    fun theNextClosingCountsOnlyThePaymentsAfterTheLastOne() = runTest {
        closings.closings = listOf(
            cashClosing(number = 1, to = "2026-11-15T20:00:00Z", createdAt = "2026-11-15T21:00:00Z"),
            cashClosing(number = 2, to = "2026-11-16T20:00:00Z", createdAt = "2026-11-16T21:00:00Z", from = "2026-11-15T20:00:00Z"),
        )

        val overview = loadCashClosing()().getOrThrow()

        assertEquals(3, overview.draft.number)
        assertEquals(listOf("order_after"), overview.draft.payments.map { it.id })
        assertEquals(listOf(2L, 1L), overview.recentClosings.map { it.number })
    }

    @Test
    fun withoutConnectionNothingIsShown() = runTest {
        payments.failure = PaymentReadError.NoConnection

        assertEquals(CashClosingError.NoConnection, loadCashClosing()().exceptionOrNull())
    }

    @Test
    fun savesTheClosingSignedByTheAdministrator() = runTest {
        val draft = loadCashClosing()().getOrThrow().draft

        val result = saveCashClosing()(draft, CashClosingType.DAILY, countedCash = 7_000, note = "Faltaron $500")

        assertTrue(result.isSuccess)
        val saved = closings.closings.single()
        assertEquals(1, saved.number)
        assertEquals(7_500, saved.expectedCash)
        assertEquals(-500, saved.difference)
        assertEquals("admin", saved.closedBy)
        assertEquals(Instant.parse("2026-11-17T20:00:00Z"), saved.to)
    }

    @Test
    fun doesNotSaveWhatIsMissing() = runTest {
        val draft = CashClosingDraft(lastClosing = null, payments = payments.payments)

        assertEquals(CashClosingError.MissingNote, saveCashClosing()(draft, CashClosingType.DAILY, 0, "").exceptionOrNull())
        assertTrue(closings.closings.isEmpty())
    }

    @Test
    fun onlyAnAdministratorCloses() = runTest {
        val waiterSession = SaveCashClosingUseCase(FakeAuthRepository(userId = "mesero"), users, closings)
        val draft = CashClosingDraft(lastClosing = null, payments = emptyList())

        assertEquals(CashClosingError.NotAllowed, waiterSession(draft, CashClosingType.MANUAL, 0, "").exceptionOrNull())
    }

    @Test
    fun aClosingSavedMeanwhileByAnotherAdministratorIsNotCoveredTwice() = runTest {
        val draft = loadCashClosing()().getOrThrow().draft
        saveCashClosing()(draft, CashClosingType.MANUAL, countedCash = 7_500, note = "")

        val second = saveCashClosing()(draft, CashClosingType.DAILY, countedCash = 7_500, note = "")

        assertEquals(CashClosingError.AlreadyClosed, second.exceptionOrNull())
        assertEquals(1, closings.closings.size)
    }
}
