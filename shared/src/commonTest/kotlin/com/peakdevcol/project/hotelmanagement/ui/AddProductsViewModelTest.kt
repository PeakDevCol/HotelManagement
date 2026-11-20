package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.AddProductsToOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeCatalog
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePendingAdditionRepository
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsEffect
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsIntent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.ProductPickerIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RF-CUR-06 and RF-CUR-13. */
class AddProductsViewModelTest : MainDispatcherTest() {
    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000)
    private val catalog = FakeCatalog(soda)
    private val orders = FakeOrderRepository().apply {
        openOrders.value = listOf(order("open"), order("closed", state = OrderState.CLOSED))
    }
    private val pendingAdditions = FakePendingAdditionRepository()

    private fun viewModel(orderId: String = "open") = AddProductsViewModel(
        orderId = orderId,
        observeOrder = ObserveOrderUseCase(orders),
        getProducts = catalog.getProducts,
        addProductsToOrder = AddProductsToOrderUseCase(FakeAuthRepository(userId = "uid-1"), orders, pendingAdditions),
    )

    private fun AddProductsViewModel.pick(intent: ProductPickerIntent) = onIntent(AddProductsIntent.Picker(intent))

    @Test
    fun addsTheChosenProductsAsNewLines() = runTest {
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(soda))
        viewModel.pick(ProductPickerIntent.Add(soda))

        viewModel.onIntent(AddProductsIntent.Confirm)

        val products = orders.openOrders.value.first { it.id == "open" }.products
        assertEquals(listOf("Cerveza Aguila", "Coca-Cola"), products.map { it.name })
        assertEquals(2, products.last().quantity)
        assertEquals(AddProductsEffect.Added(OrderDelivery.SENT), viewModel.effects.first())
        assertTrue(viewModel.state.value.picker.selection.isEmpty)
        assertTrue(pendingAdditions.additions.value.isEmpty())
    }

    @Test
    fun withoutConnectionTheProductsWaitOnTheDeviceAndAreRemembered() = runTest {
        orders.addProductsResult = Result.success(OrderDelivery.PENDING)
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(soda))

        viewModel.onIntent(AddProductsIntent.Confirm)

        assertEquals(AddProductsEffect.Added(OrderDelivery.PENDING), viewModel.effects.first())
        assertEquals("open", pendingAdditions.additions.value.single().orderId)
    }

    @Test
    fun nothingIsAddedToAnOrderThatIsNoLongerInProgress() {
        val viewModel = viewModel(orderId = "closed")
        viewModel.pick(ProductPickerIntent.Add(soda))

        viewModel.onIntent(AddProductsIntent.Confirm)

        assertFalse(viewModel.state.value.isOrderInProgress)
        assertFalse(viewModel.state.value.canConfirm)
        assertEquals(1, orders.openOrders.value.first { it.id == "closed" }.products.size)
    }

    @Test
    fun nothingIsAddedWithoutProducts() {
        val viewModel = viewModel()

        viewModel.onIntent(AddProductsIntent.Confirm)

        assertFalse(viewModel.state.value.canConfirm)
        assertEquals(1, orders.openOrders.value.first { it.id == "open" }.products.size)
    }

    @Test
    fun tellsWhenTheServerRejectsTheProducts() = runTest {
        orders.addProductsResult = Result.failure(OrderActionError.StateChanged(OrderState.CLOSED))
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(soda))

        viewModel.onIntent(AddProductsIntent.Confirm)

        assertEquals(AddProductsEffect.Failed(OrderActionError.StateChanged(OrderState.CLOSED)), viewModel.effects.first())
        assertEquals(1, viewModel.state.value.picker.selection.products.size)
    }
}
