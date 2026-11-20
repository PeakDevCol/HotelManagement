package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeCatalog
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderEffect
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderIntent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.ProductPickerIntent
import com.peakdevcol.project.hotelmanagement.ui.addorder.SaveFailure
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddOrderViewModelTest : MainDispatcherTest() {

    private val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500)
    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000)

    private val catalog = FakeCatalog(beer, soda)
    private val orders = FakeOrderRepository()
    private val auth = FakeAuthRepository(userId = "uid-1")

    private fun viewModel(saveOrder: SaveOrderUseCase = SaveOrderUseCase(auth, orders)) = AddOrderViewModel(catalog.getProducts, saveOrder)

    private fun AddOrderViewModel.pick(intent: ProductPickerIntent) = onIntent(AddOrderIntent.Picker(intent))

    private fun AddOrderViewModel.chooseDestination(kind: DestinationKind, number: String = "") {
        onIntent(AddOrderIntent.OpenDestination)
        onIntent(AddOrderIntent.SelectDestinationKind(kind))
        onIntent(AddOrderIntent.DestinationNumberChange(number))
        onIntent(AddOrderIntent.ConfirmDestination)
    }

    @Test
    fun loadsTheCatalog() {
        val picker = viewModel().state.value.picker

        assertEquals(catalog.groupOf(beer, soda), picker.catalog)
        assertFalse(picker.catalogFailed)
        assertFalse(picker.isCatalogEmpty)
        assertEquals(listOf(beer, soda), picker.visibleProducts)
    }

    @Test
    fun showsCatalogErrorAndRetries() {
        catalog.productRepository.readFailure = IllegalStateException("offline")
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.picker.catalogFailed)

        catalog.productRepository.readFailure = null
        catalog.offer(listOf(beer))
        viewModel.onIntent(AddOrderIntent.RetryCatalog)

        assertFalse(viewModel.state.value.picker.catalogFailed)
        assertEquals(catalog.groupOf(beer), viewModel.state.value.picker.catalog)
    }

    @Test
    fun tellsWhenTheInventoryHasNothingToOrderYet() {
        catalog.offer(emptyList())

        assertTrue(viewModel().state.value.picker.isCatalogEmpty)
    }

    @Test
    fun eachTouchOnAProductAddsOneUnit() {
        val viewModel = viewModel()

        viewModel.pick(ProductPickerIntent.Add(beer))
        viewModel.pick(ProductPickerIntent.Add(soda))
        viewModel.pick(ProductPickerIntent.Add(beer))

        val selection = viewModel.state.value.picker.selection
        assertEquals(listOf(2, 1), selection.products.map { it.quantity })
        assertEquals(2 * 3_500L + 3_000L, selection.totalAmount)
    }

    @Test
    fun theStepperChangesAndRemovesAProduct() {
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(beer))
        viewModel.pick(ProductPickerIntent.Increase(beer.id))
        viewModel.pick(ProductPickerIntent.Add(soda))

        viewModel.pick(ProductPickerIntent.Decrease(beer.id))
        viewModel.pick(ProductPickerIntent.Remove(soda.id))

        assertEquals(listOf(beer.id to 1), viewModel.state.value.picker.selection.products.map { it.id to it.quantity })
    }

    @Test
    fun searchingShowsTheMatchesOfEveryCategory() {
        val viewModel = viewModel()

        viewModel.pick(ProductPickerIntent.Search("coca"))

        assertEquals(listOf(soda), viewModel.state.value.picker.visibleProducts)
    }

    @Test
    fun confirmingWithoutADestinationOpensItFirst() {
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(beer))

        viewModel.onIntent(AddOrderIntent.Confirm)

        assertNotNull(viewModel.state.value.destinationDraft)
        assertTrue(orders.attempts.isEmpty())
    }

    @Test
    fun aRoomNeedsItsNumberAndThePoolDoesNot() {
        val viewModel = viewModel()
        viewModel.onIntent(AddOrderIntent.OpenDestination)

        viewModel.onIntent(AddOrderIntent.SelectDestinationKind(DestinationKind.ROOM))
        assertNull(viewModel.state.value.destinationDraft?.destination)
        viewModel.onIntent(AddOrderIntent.DestinationNumberChange("2a04"))
        assertEquals(OrderDestination.Room(204), viewModel.state.value.destinationDraft?.destination)

        viewModel.onIntent(AddOrderIntent.SelectDestinationKind(DestinationKind.POOL))
        viewModel.onIntent(AddOrderIntent.DestinationNumberChange(""))
        viewModel.onIntent(AddOrderIntent.ConfirmDestination)

        assertEquals(OrderDestination.Pool(), viewModel.state.value.destination)
        assertNull(viewModel.state.value.destinationDraft)
    }

    @Test
    fun confirmNeedsProductsAndDestination() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.canConfirm)

        viewModel.pick(ProductPickerIntent.Add(beer))
        assertFalse(viewModel.state.value.canConfirm)

        viewModel.chooseDestination(DestinationKind.ROOM, "204")
        assertTrue(viewModel.state.value.canConfirm)
    }

    @Test
    fun savingSendsTheOrderAndTellsTheScreen() = runTest {
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(beer))
        viewModel.pick(ProductPickerIntent.Increase(beer.id))
        viewModel.chooseDestination(DestinationKind.TABLE, "3")

        viewModel.onIntent(AddOrderIntent.Confirm)

        val saved = orders.saved.single()
        assertEquals(OrderDestination.Table(3), saved.destination)
        assertEquals(7_000, saved.totalAmount)
        assertEquals(AddOrderEffect.Saved(OrderDelivery.SENT), viewModel.effects.first())
        assertTrue(viewModel.state.value.picker.selection.isEmpty)
    }

    @Test
    fun failedSaveKeepsTheOrderAndRetriesWithTheSameId() = runTest {
        orders.result = Result.failure(IllegalStateException("permission denied"))
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(beer))
        viewModel.chooseDestination(DestinationKind.OTHER)

        viewModel.onIntent(AddOrderIntent.Confirm)

        assertEquals(AddOrderEffect.SaveFailed(SaveFailure.Unknown), viewModel.effects.first())
        assertFalse(viewModel.state.value.isSaving)
        assertEquals(1, viewModel.state.value.picker.selection.products.size)

        orders.result = Result.success(OrderDelivery.SENT)
        viewModel.onIntent(AddOrderIntent.Confirm)

        assertEquals(2, orders.attempts.size)
        assertEquals(orders.attempts[0].id, orders.attempts[1].id)
    }

    @Test
    fun anOrderWithoutAnswerFromTheServerIsSavedOnTheDevice() = runTest {
        orders.result = Result.success(OrderDelivery.PENDING)
        val viewModel = viewModel()
        viewModel.pick(ProductPickerIntent.Add(beer))
        viewModel.chooseDestination(DestinationKind.POOL, "12")

        viewModel.onIntent(AddOrderIntent.Confirm)

        assertEquals(AddOrderEffect.Saved(OrderDelivery.PENDING), viewModel.effects.first())
        assertEquals(OrderDestination.Pool(12), orders.saved.single().destination)
    }

    @Test
    fun theNextOrderGetsANewId() {
        val viewModel = viewModel()
        repeat(2) {
            viewModel.pick(ProductPickerIntent.Add(beer))
            viewModel.chooseDestination(DestinationKind.OTHER)
            viewModel.onIntent(AddOrderIntent.Confirm)
        }

        assertNotEquals(orders.saved[0].id, orders.saved[1].id)
    }

    @Test
    fun reportsMissingSession() = runTest {
        val viewModel = viewModel(SaveOrderUseCase(FakeAuthRepository(userId = null), orders))
        viewModel.pick(ProductPickerIntent.Add(beer))
        viewModel.chooseDestination(DestinationKind.OTHER)

        viewModel.onIntent(AddOrderIntent.Confirm)

        assertEquals(AddOrderEffect.SaveFailed(SaveFailure.NoSession), viewModel.effects.first())
    }
}
