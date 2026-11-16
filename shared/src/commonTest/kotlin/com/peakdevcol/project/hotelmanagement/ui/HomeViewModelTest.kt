package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.DismissRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReconcilePendingAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.home.HomeIntent
import com.peakdevcol.project.hotelmanagement.ui.home.HomeViewModel
import com.peakdevcol.project.hotelmanagement.ui.home.LogOutDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeViewModelTest : MainDispatcherTest() {
    private val auth = FakeAuthRepository(userId = "uid-1")
    private val orders = FakeOrderRepository()
    private val pendingAdditions = FakePendingAdditionRepository()

    private fun viewModel() =
        HomeViewModel(
            observePendingOrderCount = ObservePendingOrderCountUseCase(auth, orders),
            observeRejectedAdditions = ObserveRejectedAdditionsUseCase(pendingAdditions),
            reconcilePendingAdditions = ReconcilePendingAdditionsUseCase(orders, pendingAdditions),
            dismissRejectedAdditions = DismissRejectedAdditionsUseCase(pendingAdditions),
            logOut = LogOutUseCase(auth, orders, pendingAdditions),
        )

    @Test
    fun showsHowManyOrdersWaitToBeSent() {
        val viewModel = viewModel()
        assertEquals(0, viewModel.state.value.pendingOrderCount)

        orders.pendingOrders.value = mapOf("uid-1" to 3)

        assertEquals(3, viewModel.state.value.pendingOrderCount)
    }

    @Test
    fun loggingOutAsksForConfirmationFirst() {
        val viewModel = viewModel()

        viewModel.onIntent(HomeIntent.LogOutClick)

        assertEquals(LogOutDialog.Confirmation, viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun confirmingClosesTheSession() {
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.LogOutClick)

        viewModel.onIntent(HomeIntent.LogOutConfirm)

        assertNull(auth.currentUserId())
        assertNull(viewModel.state.value.logOutDialog)
    }

    @Test
    fun dismissingKeepsTheSession() {
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.LogOutClick)

        viewModel.onIntent(HomeIntent.LogOutDialogDismiss)

        assertNull(viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun pendingOrdersBlockTheLogOutWithAWarning() {
        orders.pendingOrders.value = mapOf("uid-1" to 1)
        val viewModel = viewModel()

        viewModel.onIntent(HomeIntent.LogOutClick)

        assertEquals(LogOutDialog.PendingOrders, viewModel.state.value.logOutDialog)
    }

    @Test
    fun anOrderThatBecomesPendingAfterTheConfirmationStillBlocks() {
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.LogOutClick)
        orders.pendingOrders.value = mapOf("uid-1" to 1)

        viewModel.onIntent(HomeIntent.LogOutConfirm)

        assertEquals(LogOutDialog.PendingOrders, viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun theMenuWorksWhenThePendingOrdersCannotBeRead() {
        orders.pendingOrdersFailure = IllegalStateException("permission denied")

        assertEquals(0, viewModel().state.value.pendingOrderCount)
    }

    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000, quantity = 1, lineId = "added-offline")
    private val addedOffline = PendingAddition("addition-1", "order-1", OrderDestination.Table(3), listOf(soda))

    @Test
    fun warnsAboutTheProductsTheServerDidNotAdd() {
        pendingAdditions.additions.value = listOf(addedOffline)
        orders.openOrders.value = listOf(order("order-1", state = OrderState.CLOSED))

        val rejected = viewModel().state.value.rejectedAdditions.single()

        assertEquals(OrderDestination.Table(3), rejected.destination)
        assertEquals(listOf(soda), rejected.products)
    }

    @Test
    fun saysNothingWhenTheServerAddedTheProducts() {
        pendingAdditions.additions.value = listOf(addedOffline)
        orders.openOrders.value = listOf(order("order-1", products = listOf(soda)))

        assertTrue(viewModel().state.value.rejectedAdditions.isEmpty())
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun theWarningIsShownOnlyUntilTheEmployeeReadsIt() {
        pendingAdditions.additions.value = listOf(addedOffline.copy(rejected = true))
        val viewModel = viewModel()
        assertEquals(1, viewModel.state.value.rejectedAdditions.size)

        viewModel.onIntent(HomeIntent.RejectedAdditionsRead)

        assertTrue(viewModel.state.value.rejectedAdditions.isEmpty())
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun productsWaitingToBeSentAlsoBlockTheLogOut() {
        pendingAdditions.additions.value = listOf(addedOffline)
        orders.openOrders.value = listOf(order("order-1", products = listOf(soda), isPendingToSend = true))
        val viewModel = viewModel()
        viewModel.onIntent(HomeIntent.LogOutClick)

        viewModel.onIntent(HomeIntent.LogOutConfirm)

        assertEquals(LogOutDialog.PendingOrders, viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }
}
