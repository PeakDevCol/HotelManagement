package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.AddProductsToOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterPaymentUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RemoveProductFromOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeProductCatalog
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailEffect
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailIntent
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailState
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000)
    private val catalog = FakeProductCatalog(Result.success(listOf(soda)))
    private val pendingAdditions = FakePendingAdditionRepository()

    private fun viewModel(orderId: String = "open", userId: String = "uid-1"): OrderDetailViewModel {
        val auth = FakeAuthRepository(userId)
        return OrderDetailViewModel(
            orderId = orderId,
            observeOrderDetail = ObserveOrderDetailUseCase(auth, orders, users),
            closeOrder = CloseOrderUseCase(auth, orders),
            reopenOrder = ReopenOrderUseCase(auth, orders),
            registerPayment = RegisterPaymentUseCase(auth, orders),
            cancelOrder = CancelOrderUseCase(auth, users, orders),
            getProducts = GetProductsUseCase(catalog),
            addProductsToOrder = AddProductsToOrderUseCase(auth, orders, pendingAdditions),
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

    @Test
    fun registersThePaymentWithItsMethodAndTip() = runTest {
        val viewModel = viewModel(orderId = "closed")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))

        viewModel.onIntent(OrderDetailIntent.SubmitPayment(PaymentMethod.CASH, tipAmount = 1_500))

        assertEquals(
            "closed" to OrderStateChange.Pay("uid-1", PaymentMethod.CASH, tipAmount = 1_500),
            orders.appliedChanges.single(),
        )
        val state = viewModel.loaded()
        assertEquals(OrderState.PAID, state.detail.order.state)
        assertTrue(state.detail.availableActions.isEmpty())
        assertEquals(OrderDetailEffect.ActionDone(OrderAction.PAY), viewModel.effects.first())
    }

    @Test
    fun anAdministratorCancelsWithAReason() = runTest {
        val viewModel = viewModel(userId = "admin")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.CANCEL))
        assertEquals(OrderAction.CANCEL, viewModel.loaded().dialog)

        viewModel.onIntent(OrderDetailIntent.SubmitCancellation("Pedido duplicado"))

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

        viewModel.onIntent(OrderDetailIntent.SubmitPayment(PaymentMethod.CASH, tipAmount = 0))

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
    fun aChangeFromTheServerKeepsTheDialogTheEmployeeIsFillingIn() {
        val viewModel = viewModel(orderId = "closed")
        viewModel.onIntent(OrderDetailIntent.ActionClick(OrderAction.PAY))

        orders.openOrders.value += order("another")

        assertEquals(OrderAction.PAY, viewModel.loaded().dialog)
    }

    @Test
    fun addingAProductLoadsTheCatalogAndAddsItAsANewLine() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(OrderDetailIntent.AddProductClick)
        assertTrue(viewModel.loaded().isAddingProduct)
        assertEquals(listOf(soda), viewModel.loaded().catalog)

        viewModel.onIntent(OrderDetailIntent.AddProduct(soda, quantity = 2))

        val state = viewModel.loaded()
        assertFalse(state.isAddingProduct)
        assertEquals(listOf("Cerveza Aguila", "Coca-Cola"), state.detail.order.products.map { it.name })
        assertEquals(2 * 3_500L + 2 * 3_000L, state.detail.order.totalAmount)
        assertEquals(OrderDetailEffect.ProductAdded(OrderDelivery.SENT), viewModel.effects.first())
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun aProductAddedWithoutConnectionStaysOnTheDeviceAndIsRemembered() = runTest {
        orders.addProductsResult = Result.success(OrderDelivery.PENDING)
        val viewModel = viewModel()

        viewModel.onIntent(OrderDetailIntent.AddProduct(soda, quantity = 1))

        assertEquals(OrderDetailEffect.ProductAdded(OrderDelivery.PENDING), viewModel.effects.first())
        val remembered = pendingAdditions.additions.value.single()
        assertEquals("open", remembered.orderId)
        assertEquals(listOf("Coca-Cola"), remembered.products.map { it.name })
    }

    @Test
    fun productsCannotBeAddedToAClosedOrder() {
        val viewModel = viewModel(orderId = "closed")

        viewModel.onIntent(OrderDetailIntent.AddProductClick)
        viewModel.onIntent(OrderDetailIntent.AddProduct(soda, quantity = 1))

        assertFalse(viewModel.loaded().isAddingProduct)
        assertEquals(1, viewModel.loaded().detail.order.products.size)
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
