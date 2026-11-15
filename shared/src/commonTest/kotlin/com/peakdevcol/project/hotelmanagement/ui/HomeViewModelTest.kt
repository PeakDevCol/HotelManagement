package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.ui.home.HomeIntent
import com.peakdevcol.project.hotelmanagement.ui.home.HomeViewModel
import com.peakdevcol.project.hotelmanagement.ui.home.LogOutDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeViewModelTest : MainDispatcherTest() {
    private val auth = FakeAuthRepository(userId = "uid-1")
    private val orders = FakeOrderRepository()

    private fun viewModel() =
        HomeViewModel(ObservePendingOrderCountUseCase(auth, orders), LogOutUseCase(auth, orders))

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
}
