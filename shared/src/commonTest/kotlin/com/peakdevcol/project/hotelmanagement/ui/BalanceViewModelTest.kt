package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.BalanceFilter
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadBalanceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingCollectionUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeCashClosingRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePaymentRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.burger
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.fakes.payment
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceBreakdown
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceEffect
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceIntent
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BalanceViewModelTest : MainDispatcherTest() {
    private val payments = FakePaymentRepository(
        payment("order_a", "2026-11-16T20:00:00Z", tipAmount = 500),
        payment("order_b", "2026-11-17T15:00:00Z", lines = listOf(burger), method = PaymentMethod.TRANSFER, receivedBy = "uid-2"),
    )
    private val orders = FakeOrderRepository()
    private val users = FakeUserRepository(employee("uid-1", "Ana"), employee("uid-2", "Beto"))
    private val now = object : Clock {
        override fun now() = Instant.parse("2026-11-17T20:00:00Z")
    }

    private fun viewModel() = BalanceViewModel(
        LoadBalanceUseCase(payments, FakeSettingsRepository(), FakeCashClosingRepository(), now),
        ObservePendingCollectionUseCase(orders),
        ObserveEmployeesUseCase(users),
    )

    private fun epochDay(year: Int, month: Int, day: Int) = CalendarDate(year, month, day).epochDay

    @Test
    fun opensOnTheCurrentMonthWithItsTotals() {
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals(BalancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)), state.period)
        assertEquals(15_900, state.summary!!.totals.totalReceived)
        assertEquals(2, state.summary!!.totals.paymentCount)
        assertEquals(listOf("Ana", "Beto"), state.employeeOptions.map { it.name })
    }

    @Test
    fun filteringDoesNotReadThePaymentsAgain() {
        val viewModel = viewModel()

        viewModel.onIntent(BalanceIntent.FilterByMethod(PaymentMethod.TRANSFER))

        assertEquals(11_900, viewModel.state.value.summary!!.totals.totalReceived)
        assertEquals(1, payments.reads.size)
    }

    @Test
    fun removingAFilterShowsEverythingAgain() {
        val viewModel = viewModel()
        viewModel.onIntent(BalanceIntent.FilterByEmployee("uid-1"))

        viewModel.onIntent(BalanceIntent.FilterByEmployee(null))

        assertEquals(2, viewModel.state.value.summary!!.totals.paymentCount)
    }

    @Test
    fun aNewPeriodIsReadAndClosesThePicker() {
        val viewModel = viewModel()
        viewModel.onIntent(BalanceIntent.OpenPeriodPicker)

        viewModel.onIntent(BalanceIntent.ChangePeriod(epochDay(2026, 11, 17), epochDay(2026, 11, 17)))

        val state = viewModel.state.value
        assertFalse(state.isPickingPeriod)
        assertEquals(BalancePeriod(CalendarDate(2026, 11, 17), CalendarDate(2026, 11, 17)), state.period)
        assertEquals(listOf("order_b"), state.report!!.payments.map { it.id })
    }

    @Test
    fun aPeriodLongerThanAMonthIsRejectedAndTheCurrentOneStays() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(BalanceIntent.ChangePeriod(epochDay(2026, 9, 1), epochDay(2026, 11, 30)))

        assertEquals(BalanceEffect.PeriodRejected(BalanceError.PeriodTooLong), viewModel.effects.first())
        assertEquals(BalancePeriod(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 30)), viewModel.state.value.period)
        assertEquals(1, payments.reads.size)
    }

    @Test
    fun aFilterThatIsNotInTheNewPeriodIsDropped() {
        val viewModel = viewModel()
        viewModel.onIntent(BalanceIntent.FilterByProduct("beer"))

        viewModel.onIntent(BalanceIntent.ChangePeriod(epochDay(2026, 11, 17), epochDay(2026, 11, 17)))

        assertNull(viewModel.state.value.filter.productId)
    }

    @Test
    fun withoutConnectionItSaysSoInsteadOfShowingZero() {
        payments.failure = PaymentReadError.NoConnection

        val state = viewModel().state.value

        assertEquals(PaymentReadError.NoConnection, state.loadError)
        assertNull(state.summary)
    }

    @Test
    fun retryingAfterAFailureShowsTheBalance() {
        payments.failure = PaymentReadError.NoConnection
        val viewModel = viewModel()
        payments.failure = null

        viewModel.onIntent(BalanceIntent.Reload)

        assertNull(viewModel.state.value.loadError)
        assertEquals(2, viewModel.state.value.summary!!.totals.paymentCount)
    }

    @Test
    fun showsWhatIsStillToBeCollectedAsItChanges() {
        val viewModel = viewModel()
        assertEquals(0, viewModel.state.value.pending!!.totalAmount)

        orders.openOrders.value = listOf(order("x", state = OrderState.CLOSED))

        assertEquals(7_000, viewModel.state.value.pending!!.closedAmount)
    }

    @Test
    fun marksTheDaysThatEndedWithoutADailyClosing() {
        // order_a is from 16 nov, which has ended; order_b is from today.
        assertEquals(setOf(CalendarDate(2026, 11, 16)), viewModel().state.value.daysWithoutDailyClosing)
    }

    @Test
    fun switchesTheBreakdown() {
        val viewModel = viewModel()

        viewModel.onIntent(BalanceIntent.ShowBreakdown(BalanceBreakdown.BY_PRODUCT))

        assertEquals(BalanceBreakdown.BY_PRODUCT, viewModel.state.value.breakdown)
        assertTrue(viewModel.state.value.summary!!.byProduct.isNotEmpty())
    }

    @Test
    fun theFiltersOpenInTheirOwnSheetAndAreClearedTogether() {
        val viewModel = viewModel()
        viewModel.onIntent(BalanceIntent.OpenFilters)
        assertTrue(viewModel.state.value.isChoosingFilters)

        viewModel.onIntent(BalanceIntent.FilterByMethod(PaymentMethod.CASH))
        viewModel.onIntent(BalanceIntent.FilterByEmployee("uid-1"))
        viewModel.onIntent(BalanceIntent.DismissFilters)
        assertFalse(viewModel.state.value.isChoosingFilters)
        assertEquals(2, viewModel.state.value.activeFilterCount)

        viewModel.onIntent(BalanceIntent.ClearFilters)

        assertEquals(0, viewModel.state.value.activeFilterCount)
        assertEquals(BalanceFilter(), viewModel.state.value.filter)
    }
}
