package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeProductCatalog
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderIntent
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderViewModel
import com.peakdevcol.project.hotelmanagement.ui.addorder.SaveFailure
import com.peakdevcol.project.hotelmanagement.ui.addorder.SaveResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddOrderViewModelTest : MainDispatcherTest() {

    private val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500)
    private val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000)

    private val catalog = FakeProductCatalog(Result.success(listOf(beer, soda)))
    private val orders = FakeOrderRepository()
    private val auth = FakeAuthRepository(userId = "uid-1")

    private fun viewModel() = AddOrderViewModel(GetProductsUseCase(catalog), SaveOrderUseCase(auth, orders))

    @Test
    fun loadsTheCatalog() {
        val state = viewModel().state.value

        assertEquals(listOf(beer, soda), state.catalog)
        assertFalse(state.catalogFailed)
    }

    @Test
    fun showsCatalogErrorAndRetries() {
        catalog.result = Result.failure(IllegalStateException("offline"))
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.catalogFailed)

        catalog.result = Result.success(listOf(beer))
        viewModel.onIntent(AddOrderIntent.RetryCatalog)

        assertFalse(viewModel.state.value.catalogFailed)
        assertEquals(listOf(beer), viewModel.state.value.catalog)
    }

    @Test
    fun addingTheSameProductTwiceAddsItsQuantities() {
        val viewModel = viewModel()

        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 2))
        viewModel.onIntent(AddOrderIntent.AddProduct(soda, 1))
        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 3))

        val products = viewModel.state.value.products
        assertEquals(listOf(5, 1), products.map { it.quantity })
        assertEquals(5 * 3_500L + 3_000L, viewModel.state.value.totalAmount)
    }

    @Test
    fun ignoresQuantitiesBelowOne() {
        val viewModel = viewModel()

        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 0))

        assertTrue(viewModel.state.value.products.isEmpty())
    }

    @Test
    fun removesAProduct() {
        val viewModel = viewModel()
        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 2))
        viewModel.onIntent(AddOrderIntent.AddProduct(soda, 1))

        viewModel.onIntent(AddOrderIntent.RemoveProduct(beer.id))

        assertEquals(listOf(soda.id), viewModel.state.value.products.map { it.id })
    }

    @Test
    fun confirmNeedsProductsAndDestination() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.canConfirm)

        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 1))
        assertFalse(viewModel.state.value.canConfirm)

        viewModel.onIntent(AddOrderIntent.SelectDestination(OrderDestination.Room(204)))
        assertTrue(viewModel.state.value.canConfirm)
    }

    @Test
    fun savingResetsTheOrderButKeepsTheCatalog() {
        val viewModel = viewModel()
        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 2))
        viewModel.onIntent(AddOrderIntent.SelectDestination(OrderDestination.Table(3)))

        viewModel.onIntent(AddOrderIntent.Confirm)

        val saved = orders.saved.single()
        assertEquals(OrderDestination.Table(3), saved.destination)
        assertEquals(7_000, saved.totalAmount)
        val state = viewModel.state.value
        assertEquals(SaveResult.Sent, state.saveResult)
        assertTrue(state.products.isEmpty())
        assertNull(state.destination)
        assertEquals(listOf(beer, soda), state.catalog)

        viewModel.onIntent(AddOrderIntent.DismissResult)
        assertNull(viewModel.state.value.saveResult)
    }

    @Test
    fun failedSaveKeepsTheOrderAndRetriesWithTheSameId() {
        orders.result = Result.failure(IllegalStateException("permission denied"))
        val viewModel = viewModel()
        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 1))
        viewModel.onIntent(AddOrderIntent.SelectDestination(OrderDestination.Other))

        viewModel.onIntent(AddOrderIntent.Confirm)

        val state = viewModel.state.value
        assertEquals(SaveResult.Failed(SaveFailure.Unknown), state.saveResult)
        assertFalse(state.isSaving)
        assertEquals(1, state.products.size)

        orders.result = Result.success(OrderDelivery.SENT)
        viewModel.onIntent(AddOrderIntent.Confirm)

        assertEquals(2, orders.attempts.size)
        assertEquals(orders.attempts[0].id, orders.attempts[1].id)
    }

    @Test
    fun anOrderWithoutAnswerFromTheServerIsSavedOnTheDeviceAndTheScreenIsReadyForTheNext() {
        orders.result = Result.success(OrderDelivery.PENDING)
        val viewModel = viewModel()
        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 1))
        viewModel.onIntent(AddOrderIntent.SelectDestination(OrderDestination.Pool(12)))

        viewModel.onIntent(AddOrderIntent.Confirm)

        val state = viewModel.state.value
        assertEquals(SaveResult.SavedOnDevice, state.saveResult)
        assertTrue(state.products.isEmpty())
        assertFalse(state.isSaving)
        assertEquals(OrderDestination.Pool(12), orders.saved.single().destination)
    }

    @Test
    fun nextOrderGetsANewId() {
        val viewModel = viewModel()
        repeat(2) {
            viewModel.onIntent(AddOrderIntent.AddProduct(beer, 1))
            viewModel.onIntent(AddOrderIntent.SelectDestination(OrderDestination.Other))
            viewModel.onIntent(AddOrderIntent.Confirm)
        }

        assertNotEquals(orders.saved[0].id, orders.saved[1].id)
    }

    @Test
    fun reportsMissingSession() {
        val viewModel = AddOrderViewModel(
            GetProductsUseCase(catalog),
            SaveOrderUseCase(FakeAuthRepository(userId = null), orders),
        )
        viewModel.onIntent(AddOrderIntent.AddProduct(beer, 1))
        viewModel.onIntent(AddOrderIntent.SelectDestination(OrderDestination.Other))

        viewModel.onIntent(AddOrderIntent.Confirm)

        assertEquals(SaveResult.Failed(SaveFailure.NoSession), viewModel.state.value.saveResult)
    }
}
