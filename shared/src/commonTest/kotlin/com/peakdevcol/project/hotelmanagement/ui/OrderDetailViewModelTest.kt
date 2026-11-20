package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ChargeOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RemoveProductFromOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailEffect
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailIntent
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailState
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailViewModel
import com.peakdevcol.project.hotelmanagement.ui.currentorders.TipChoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderDetailViewModelTest : MainDispatcherTest() {
    private val orders = FakeOrderRepository().apply {
        openOrders.value = listOf(order("open"), order("closed", state = OrderState.CLOSED))
    }
    private val users = FakeUserRepository(
        employee("uid-1", "Ana Gómez"),
        employee("admin", "Juan Felipe", type = UserType.ADMIN),
    )

    private fun viewModel(orderId: String = "open", userId: String = "uid-1"): OrderDetailViewModel {
        val auth = FakeAuthRepository(userId)
        return OrderDetailViewModel(
            orderId = orderId,
            observeOrderDetail = ObserveOrderDetailUseCase(auth, orders, users),
            closeOrder = CloseOrderUseCase(auth, orders),
            reopenOrder = ReopenOrderUseCase(auth, orders),
            chargeOrder = ChargeOrderUseCase(auth, orders),
            cancelOrder = CancelOrderUseCase(auth, users, orders),
            removeProductFromOrder = RemoveProductFromOrderUseCase(auth, orders),
        )
    }

    private fun OrderDetailViewModel.loaded() = assertIs<OrderDetailState.Loaded>(state.value)

    @Test
    fun showsTheOrder() {
        val state = viewModel().loaded()

        assertEquals("open", state.detail.order.id)
        assertEquals("Ana Gómez", state.detail.employeeName)
    }

    @Test
    fun tellsWhenTheOrderDoesNotExist() {
        assertEquals(OrderDetailState.NotFound, viewModel(orderId = "missing").state.value)
    }

    @Test
    fun showsAnErrorAndRetries() {
        orders.openOrdersFailure = IllegalStateException("permission denied")
        val viewModel = viewModel()
        assertEquals(OrderDetailState.LoadFailed, viewModel.state.value)

        orders.openOrdersFailure = null
        viewModel.onIntent(OrderDetailIntent.Retry)

        viewModel.loaded()
    }

    @Test
    fun closingAsksForConfirmationAndThenClosesTheBill() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.CLOSE))
        assertEquals(OrderAction.CLOSE, viewModel.loaded().dialog)
        assertTrue(orders.appliedChanges.isEmpty())

        viewModel.onIntent(OrderDetailIntent.ConfirmClose)

        val state = viewModel.loaded()
        assertEquals(OrderState.CLOSED, state.detail.order.state)
        assertNull(state.dialog)
        assertFalse(state.isWorking)
        assertEquals(OrderDetailEffect.ActionDone(OrderAction.CLOSE), viewModel.effects.first())
    }

    @Test
    fun dismissingTheDialogChangesNothing() {
        val viewModel = viewModel()
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.CLOSE))

        viewModel.onIntent(OrderDetailIntent.DismissDialog)

        assertNull(viewModel.loaded().dialog)
        assertTrue(orders.appliedChanges.isEmpty())
    }

    @Test
    fun reopensAClosedOrder() = runTest {
        val viewModel = viewModel(orderId = "closed")

        viewModel.onIntent(OrderDetailIntent.ConfirmReopen)

        assertEquals(OrderState.IN_PROGRESS, viewModel.loaded().detail.order.state)
        assertEquals(OrderDetailEffect.ActionDone(OrderAction.REOPEN), viewModel.effects.first())
    }

    private fun OrderDetailViewModel.charge(method: PaymentMethod, tip: TipChoice = TipChoice.NONE, otherTip: String = "") {
        onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))
        onIntent(OrderDetailIntent.SelectPaymentMethod(method))
        onIntent(OrderDetailIntent.SelectTip(tip))
        if (otherTip.isNotEmpty()) onIntent(OrderDetailIntent.OtherTipChange(otherTip))
        onIntent(OrderDetailIntent.SubmitCharge)
    }

    @Test
    fun chargingRecordsTheMethodAndTheTip() = runTest {
        val viewModel = viewModel(orderId = "closed")

        viewModel.charge(PaymentMethod.CASH, TipChoice.OTHER, otherTip = "1.500")

        assertEquals(
            "closed" to OrderStateChange.Pay("uid-1", PaymentMethod.CASH, tipAmount = 1_500),
            orders.appliedChanges.single(),
        )
        val state = viewModel.loaded()
        assertEquals(OrderState.PAID, state.detail.order.state)
        assertTrue(state.detail.availableActions.isEmpty())
        assertNull(state.charge)
        assertEquals(OrderDetailEffect.ActionDone(OrderAction.PAY), viewModel.effects.first())
    }

    @Test
    fun anOrderInProgressIsClosedAndChargedInOneStep() = runTest {
        val viewModel = viewModel(orderId = "open")
        assertTrue(OrderAction.PAY in viewModel.loaded().detail.availableActions)

        viewModel.charge(PaymentMethod.TRANSFER)

        assertEquals(
            listOf(
                "open" to OrderStateChange.Close("uid-1"),
                "open" to OrderStateChange.Pay("uid-1", PaymentMethod.TRANSFER, tipAmount = 0),
            ),
            orders.appliedChanges,
        )
        assertEquals(OrderDetailEffect.ActionDone(OrderAction.PAY), viewModel.effects.first())
    }

    @Test
    fun theChargeStartsWithoutTipAndWithoutMethod() {
        val viewModel = viewModel(orderId = "closed")

        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))

        val state = viewModel.loaded()
        val form = assertNotNull(state.charge)
        assertEquals(TipChoice.NONE, form.tip)
        assertNull(form.method)
        assertFalse(state.canSubmitCharge)
        assertEquals(state.detail.order.totalAmount, state.amountToReceive)
    }

    @Test
    fun theSuggestedTipIsTenPercentWithoutTaxesRoundedDown() {
        val lunch = Product(id = "9", name = "Almuerzo", unitPrice = 46_000, quantity = 1, lineId = "lunch", taxRate = 800)
        orders.openOrders.value = listOf(order("lunch", state = OrderState.CLOSED, products = listOf(lunch)))
        val viewModel = viewModel(orderId = "lunch")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))

        viewModel.onIntent(OrderDetailIntent.SelectTip(TipChoice.SUGGESTED))

        // $46.000 with 8 % included is $42.593 without it: 10 % is $4.259, rounded down to $4.200.
        val state = viewModel.loaded()
        assertEquals(4_200, state.suggestedTip)
        assertEquals(46_000 + 4_200L, state.amountToReceive)
    }

    @Test
    fun anotherValueNeedsAnAmount() {
        val viewModel = viewModel(orderId = "closed")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))
        viewModel.onIntent(OrderDetailIntent.SelectPaymentMethod(PaymentMethod.CASH))

        viewModel.onIntent(OrderDetailIntent.SelectTip(TipChoice.OTHER))
        assertFalse(viewModel.loaded().canSubmitCharge)

        viewModel.onIntent(OrderDetailIntent.OtherTipChange("2000"))
        assertTrue(viewModel.loaded().canSubmitCharge)
        assertEquals(2_000, viewModel.loaded().tipAmount)
    }

    @Test
    fun nothingIsChargedWithoutAMethod() {
        val viewModel = viewModel(orderId = "closed")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))

        viewModel.onIntent(OrderDetailIntent.SubmitCharge)

        assertTrue(orders.appliedChanges.isEmpty())
        assertNotNull(viewModel.loaded().charge)
    }

    @Test
    fun anAdministratorCancelsWithAReason() = runTest {
        val viewModel = viewModel(userId = "admin")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.CANCEL))
        assertEquals(OrderAction.CANCEL, viewModel.loaded().dialog)
        assertFalse(viewModel.loaded().canSubmitCancellation)

        viewModel.onIntent(OrderDetailIntent.CancelReasonChange("Pedido duplicado"))
        viewModel.onIntent(OrderDetailIntent.SubmitCancellation)

        assertEquals(OrderState.CANCELLED, viewModel.loaded().detail.order.state)
        assertEquals(OrderDetailEffect.ActionDone(OrderAction.CANCEL), viewModel.effects.first())
    }

    @Test
    fun anEmployeeIsNotOfferedTheCancellation() {
        val viewModel = viewModel(userId = "uid-1")

        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.CANCEL))

        assertFalse(OrderAction.CANCEL in viewModel.loaded().detail.availableActions)
        assertNull(viewModel.loaded().dialog)
    }

    @Test
    fun warnsWhenAnotherDeviceChangedTheOrderFirst() = runTest {
        val viewModel = viewModel(orderId = "closed")
        orders.changeFailure = OrderActionError.StateChanged(OrderState.PAID)

        viewModel.charge(PaymentMethod.CASH)

        assertEquals(
            OrderDetailEffect.ActionFailed(OrderActionError.StateChanged(OrderState.PAID)),
            viewModel.effects.first(),
        )
        assertFalse(viewModel.loaded().isWorking)
    }

    @Test
    fun withoutConnectionTheActionsAreNotAvailable() {
        orders.openOrders.value = listOf(order("open", isUpToDate = false))
        val viewModel = viewModel()

        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.CLOSE))

        val state = viewModel.loaded()
        assertTrue(state.isOffline)
        assertFalse(state.canChangeState)
        assertNull(state.dialog)
    }

    @Test
    fun aChangeFromTheServerKeepsTheChargeTheEmployeeIsFillingIn() {
        val viewModel = viewModel(orderId = "closed")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))
        viewModel.onIntent(OrderDetailIntent.SelectPaymentMethod(PaymentMethod.TRANSFER))

        orders.openOrders.value += order("another")

        assertEquals(PaymentMethod.TRANSFER, viewModel.loaded().charge?.method)
    }

    @Test
    fun removingAProductAsksFirstAndThenTakesItOut() = runTest {
        val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 2, lineId = "beer")
        val water = Product(id = "5", name = "Agua", unitPrice = 2_000, quantity = 1, lineId = "water")
        orders.openOrders.value = listOf(order("open", products = listOf(beer, water)))
        val viewModel = viewModel()

        viewModel.onIntent(OrderDetailIntent.RemoveProductClick(water))
        assertEquals(water, viewModel.loaded().productToRemove)

        viewModel.onIntent(OrderDetailIntent.ConfirmRemoveProduct)

        assertEquals(listOf(beer), viewModel.loaded().detail.order.products)
        assertEquals(OrderDetailEffect.ProductRemoved, viewModel.effects.first())
    }

    @Test
    fun theLastProductOfAnOrderCannotBeRemoved() {
        val viewModel = viewModel()
        val onlyProduct = viewModel.loaded().detail.order.products.single()

        viewModel.onIntent(OrderDetailIntent.RemoveProductClick(onlyProduct))

        assertFalse(viewModel.loaded().detail.canRemoveProducts)
        assertNull(viewModel.loaded().productToRemove)
    }

    @Test
    fun productsCannotBeRemovedWithoutConnection() {
        val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, lineId = "beer")
        val water = Product(id = "5", name = "Agua", unitPrice = 2_000, lineId = "water")
        orders.openOrders.value = listOf(order("open", products = listOf(beer, water), isUpToDate = false))
        val viewModel = viewModel()

        viewModel.onIntent(OrderDetailIntent.RemoveProductClick(water))

        assertNull(viewModel.loaded().productToRemove)
        assertTrue(viewModel.loaded().detail.canEditProducts)
    }
}
