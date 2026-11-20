package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderOwnerFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenOrdersUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersEffect
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersIntent
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class CurrentOrdersViewModelTest : MainDispatcherTest() {
    private val orders = FakeOrderRepository().apply {
        openOrders.value = listOf(
            order("mine-open", employeeId = "uid-1", createdAt = "2026-11-15T01:00:00Z"),
            order("mine-closed", employeeId = "uid-1", state = OrderState.CLOSED, createdAt = "2026-11-15T02:00:00Z"),
            order("other-open", employeeId = "uid-2", createdAt = "2026-11-15T03:00:00Z"),
        )
    }
    private val users = FakeUserRepository(employee("uid-1", "Ana Gómez"), employee("uid-2", "Luis Pérez"))

    // A fixed time: the real one ticks forever, and a test has to end.
    private fun viewModel() = CurrentOrdersViewModel(
        observeOpenOrders = ObserveOpenOrdersUseCase(FakeAuthRepository(userId = "uid-1"), orders, users),
        minuteTicks = flowOf(Instant.parse("2026-11-15T06:00:00Z")),
    )

    private fun CurrentOrdersViewModel.visibleIds() = state.value.visibleOrders.map { it.order.id }

    @Test
    fun startsShowingEveryOpenOrder() {
        val viewModel = viewModel()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(listOf("mine-open", "mine-closed", "other-open"), viewModel.visibleIds())
    }

    @Test
    fun anOrderCreatedOnAnotherDeviceAppearsWithoutReloading() {
        val viewModel = viewModel()

        orders.openOrders.value += order("new", employeeId = "uid-2", createdAt = "2026-11-15T04:00:00Z")

        assertEquals("new", viewModel.visibleIds().last())
    }

    @Test
    fun aPaidOrderLeavesTheList() {
        val viewModel = viewModel()

        orders.openOrders.value = orders.openOrders.value.filterNot { it.id == "mine-closed" }

        assertEquals(listOf("mine-open", "other-open"), viewModel.visibleIds())
    }

    @Test
    fun filtersMyOrders() {
        val viewModel = viewModel()

        viewModel.onIntent(CurrentOrdersIntent.SelectOwner(OrderOwnerFilter.MINE))

        assertEquals(listOf("mine-open", "mine-closed"), viewModel.visibleIds())
    }

    @Test
    fun filtersByStateAndTouchingItAgainShowsEveryState() {
        val viewModel = viewModel()

        viewModel.onIntent(CurrentOrdersIntent.ToggleState(OrderState.CLOSED))
        assertEquals(listOf("mine-closed"), viewModel.visibleIds())

        viewModel.onIntent(CurrentOrdersIntent.ToggleState(OrderState.CLOSED))
        assertEquals(3, viewModel.visibleIds().size)
    }

    @Test
    fun eachFilterSaysHowManyOrdersItWouldShow() {
        val viewModel = viewModel()
        viewModel.onIntent(CurrentOrdersIntent.SelectOwner(OrderOwnerFilter.MINE))

        val state = viewModel.state.value
        assertEquals(3, state.countWith(state.filter.copy(owner = OrderOwnerFilter.ALL)))
        assertEquals(2, state.countWith(state.filter.copy(owner = OrderOwnerFilter.MINE)))
        assertEquals(1, state.countWith(state.filter.copy(state = OrderState.IN_PROGRESS)))
        assertEquals(1, state.countWith(state.filter.copy(state = OrderState.CLOSED)))
    }

    @Test
    fun searchesByWhoTookTheOrderAndClosingTheSearchShowsThemAll() {
        val viewModel = viewModel()
        viewModel.onIntent(CurrentOrdersIntent.OpenSearch)
        assertTrue(viewModel.state.value.isSearchOpen)

        viewModel.onIntent(CurrentOrdersIntent.Search("luis"))
        assertEquals(listOf("other-open"), viewModel.visibleIds())

        viewModel.onIntent(CurrentOrdersIntent.CloseSearch)
        assertFalse(viewModel.state.value.isSearchOpen)
        assertEquals(3, viewModel.visibleIds().size)
    }

    @Test
    fun openingAnOrderNavigatesToItsDetail() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(CurrentOrdersIntent.OpenOrder("other-open", besideList = false))

        assertEquals(CurrentOrdersEffect.NavigateToOrder("other-open"), viewModel.effects.first())
    }

    @Test
    fun onWideWindowsTheOrderOpensBesideTheList() {
        val viewModel = viewModel()

        viewModel.onIntent(CurrentOrdersIntent.OpenOrder("other-open", besideList = true))

        assertEquals("other-open", viewModel.state.value.selectedOrderId)
    }

    @Test
    fun newOrderOpensItsScreen() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(CurrentOrdersIntent.NewOrder)

        assertEquals(CurrentOrdersEffect.NavigateToNewOrder, viewModel.effects.first())
    }

    @Test
    fun countsTheOrdersWaitingToBeSent() {
        orders.openOrders.value += order("offline", isPendingToSend = true, createdAt = "2026-11-15T05:00:00Z")

        assertEquals(1, viewModel().state.value.pendingToSendCount)
    }

    @Test
    fun showsAnErrorAndRetries() {
        orders.openOrdersFailure = IllegalStateException("permission denied")
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.loadFailed)

        orders.openOrdersFailure = null
        viewModel.onIntent(CurrentOrdersIntent.Retry)

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(3, viewModel.visibleIds().size)
    }
}
