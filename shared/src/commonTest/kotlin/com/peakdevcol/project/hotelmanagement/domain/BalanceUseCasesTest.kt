package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.PendingCollection
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadBalanceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingCollectionUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeCashClosingRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePaymentRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.cashClosing
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.fakes.payment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BalanceUseCasesTest {
    private val payments = FakePaymentRepository(
        payment("order_oct", "2026-11-01T10:59:00Z"),
        payment("order_nov", "2026-11-01T11:00:00Z"),
        payment("order_dec", "2026-12-01T11:00:00Z"),
    )
    private val settings = FakeSettingsRepository()
    private val closings = FakeCashClosingRepository(
        cashClosing(number = 1, to = "2026-10-31T20:00:00Z", createdAt = "2026-11-01T02:00:00Z"),
        cashClosing(number = 2, to = "2026-11-16T20:00:00Z", createdAt = "2026-11-16T22:00:00Z", from = "2026-10-31T20:00:00Z"),
    )
    private val now = object : Clock {
        override fun now() = Instant.parse("2026-11-17T15:00:00Z")
    }

    private fun loadBalance() = LoadBalanceUseCase(payments, settings, closings, now)

    @Test
    fun opensOnTheCurrentMonthByOperationalDays() = runTest {
        val report = loadBalance()().getOrThrow()

        assertEquals(BalancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)), report.period)
        // 5:59 a. m. of 1 nov still belongs to October, and 6:00 a. m. of 1 dec to December.
        assertEquals(listOf("order_nov"), report.payments.map { it.id })
    }

    @Test
    fun bringsTheClosingsSavedDuringThePeriodAndToday() = runTest {
        val report = loadBalance()().getOrThrow()

        // The first closing was saved at 9:00 p. m. of 31 oct in Colombia, before the period.
        assertEquals(listOf(2L), report.closings.map { it.number })
        assertEquals(CalendarDate(2026, 11, 17), report.today)
    }

    @Test
    fun withoutConnectionToTheClosingsNothingIsShown() = runTest {
        closings.failure = CashClosingError.NoConnection

        assertEquals(PaymentReadError.NoConnection, loadBalance()().exceptionOrNull())
    }

    @Test
    fun usesTheStartOfTheDayOfTheSettings() = runTest {
        settings.settings.value = HotelSettings(operationalDayStartMinutes = 0)

        val report = loadBalance()().getOrThrow()

        assertEquals(0, report.dayStartMinutes)
        assertEquals(listOf("order_oct", "order_nov"), report.payments.map { it.id })
    }

    @Test
    fun readsOnlyTheChosenPeriod() = runTest {
        val period = BalancePeriod(CalendarDate(2026, 11, 10), CalendarDate(2026, 11, 12))

        loadBalance()(period)

        assertEquals(listOf(Instant.parse("2026-11-10T11:00:00Z") to Instant.parse("2026-11-13T11:00:00Z")), payments.reads)
    }

    @Test
    fun rejectsPeriodsLongerThanAMonthWithoutReading() = runTest {
        val result = loadBalance()(BalancePeriod(CalendarDate(2026, 10, 1), CalendarDate(2026, 11, 1)))

        assertEquals(BalanceError.PeriodTooLong, result.exceptionOrNull())
        assertTrue(payments.reads.isEmpty())
    }

    @Test
    fun acceptsAMonthOfThirtyOneDays() = runTest {
        val result = loadBalance()(BalancePeriod(CalendarDate(2026, 12, 1), CalendarDate(2026, 12, 31)))

        assertTrue(result.isSuccess)
    }

    @Test
    fun rejectsAPeriodThatEndsBeforeItStarts() = runTest {
        val result = loadBalance()(BalancePeriod(CalendarDate(2026, 11, 12), CalendarDate(2026, 11, 10)))

        assertEquals(BalanceError.InvalidPeriod, result.exceptionOrNull())
    }

    @Test
    fun tellsWhenTheServerCannotBeReached() = runTest {
        payments.failure = PaymentReadError.NoConnection

        assertEquals(PaymentReadError.NoConnection, loadBalance()().exceptionOrNull())
    }

    @Test
    fun theAmountToCollectAddsUpTheOrdersInProgressAndClosed() = runTest {
        val orders = FakeOrderRepository()
        orders.openOrders.value = listOf(
            order("a", state = OrderState.IN_PROGRESS),
            order("b", state = OrderState.IN_PROGRESS),
            order("c", state = OrderState.CLOSED),
        )

        val pending = ObservePendingCollectionUseCase(orders)().first()

        assertEquals(PendingCollection(inProgressCount = 2, inProgressAmount = 14_000, closedCount = 1, closedAmount = 7_000), pending)
        assertEquals(21_000, pending.totalAmount)
    }
}
