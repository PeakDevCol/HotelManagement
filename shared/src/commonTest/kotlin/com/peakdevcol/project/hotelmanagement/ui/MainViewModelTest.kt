package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.DismissRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRejectedAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReconcilePendingAdditionsUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.main.LogOutDialog
import com.peakdevcol.project.hotelmanagement.ui.main.MainIntent
import com.peakdevcol.project.hotelmanagement.ui.main.MainViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MainViewModelTest : MainDispatcherTest() {
    private var auth = FakeAuthRepository(userId = "uid-1")
    private val orders = FakeOrderRepository()
    private val users = FakeUserRepository(employee("uid-1", "Ana Gómez"), employee("admin", "Juan Felipe", type = UserType.ADMIN))
    private val pendingAdditions = FakePendingAdditionRepository()

    private fun viewModel() =
        MainViewModel(
            observePendingOrderCount = ObservePendingOrderCountUseCase(auth, orders),
            observeAccess = ObserveAccessUseCase(auth, users),
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

        viewModel.onIntent(MainIntent.LogOutClick)

        assertEquals(LogOutDialog.Confirmation, viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun confirmingClosesTheSession() {
        val viewModel = viewModel()
        viewModel.onIntent(MainIntent.LogOutClick)

        viewModel.onIntent(MainIntent.LogOutConfirm)

        assertNull(auth.currentUserId())
        assertNull(viewModel.state.value.logOutDialog)
    }

    @Test
    fun dismissingKeepsTheSession() {
        val viewModel = viewModel()
        viewModel.onIntent(MainIntent.LogOutClick)

        viewModel.onIntent(MainIntent.LogOutDialogDismiss)

        assertNull(viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun pendingOrdersBlockTheLogOutWithAWarning() {
        orders.pendingOrders.value = mapOf("uid-1" to 1)
        val viewModel = viewModel()

        viewModel.onIntent(MainIntent.LogOutClick)

        assertEquals(LogOutDialog.PendingOrders, viewModel.state.value.logOutDialog)
    }

    @Test
    fun anOrderThatBecomesPendingAfterTheConfirmationStillBlocks() {
        val viewModel = viewModel()
        viewModel.onIntent(MainIntent.LogOutClick)
        orders.pendingOrders.value = mapOf("uid-1" to 1)

        viewModel.onIntent(MainIntent.LogOutConfirm)

        assertEquals(LogOutDialog.PendingOrders, viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun theAppWorksWhenThePendingOrdersCannotBeRead() {
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

        viewModel.onIntent(MainIntent.RejectedAdditionsRead)

        assertTrue(viewModel.state.value.rejectedAdditions.isEmpty())
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun productsWaitingToBeSentAlsoBlockTheLogOut() {
        pendingAdditions.additions.value = listOf(addedOffline)
        orders.openOrders.value = listOf(order("order-1", products = listOf(soda), isPendingToSend = true))
        val viewModel = viewModel()
        viewModel.onIntent(MainIntent.LogOutClick)

        viewModel.onIntent(MainIntent.LogOutConfirm)

        assertEquals(LogOutDialog.PendingOrders, viewModel.state.value.logOutDialog)
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun theNavigationShowsWhatTheRoleAllows() {
        val employeeState = viewModel().state.value
        assertEquals("Ana Gómez", employeeState.employee?.fullName)
        assertEquals(listOf(AppSection.ORDERS), employeeState.navigation?.sections)

        auth = FakeAuthRepository(userId = "admin")

        val sections = viewModel().state.value.navigation?.sections.orEmpty()
        assertTrue(AppSection.EMPLOYEES in sections)
        assertTrue(AppSection.INVENTORY in sections)
    }
}
