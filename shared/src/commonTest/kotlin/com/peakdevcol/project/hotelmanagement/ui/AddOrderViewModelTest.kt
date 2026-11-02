package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.OrderError
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeProductCatalog
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
        val state = viewModel().uiState.value

        assertEquals(listOf(beer, soda), state.catalog)
        assertFalse(state.catalogFailed)
    }

    @Test
    fun showsCatalogErrorAndRetries() {
        catalog.result = Result.failure(IllegalStateException("offline"))
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.catalogFailed)

        catalog.result = Result.success(listOf(beer))
        viewModel.loadCatalog()

        assertFalse(viewModel.uiState.value.catalogFailed)
        assertEquals(listOf(beer), viewModel.uiState.value.catalog)
    }

    @Test
    fun addingTheSameProductTwiceAddsItsQuantities() {
        val viewModel = viewModel()

        viewModel.onAddProduct(beer, 2)
        viewModel.onAddProduct(soda, 1)
        viewModel.onAddProduct(beer, 3)

        val products = viewModel.uiState.value.products
        assertEquals(listOf(5, 1), products.map { it.quantity })
        assertEquals(5 * 3_500L + 3_000L, viewModel.uiState.value.totalAmount)
    }

    @Test
    fun ignoresQuantitiesBelowOne() {
        val viewModel = viewModel()

        viewModel.onAddProduct(beer, 0)

        assertTrue(viewModel.uiState.value.products.isEmpty())
    }

    @Test
    fun removesAProduct() {
        val viewModel = viewModel()
        viewModel.onAddProduct(beer, 2)
        viewModel.onAddProduct(soda, 1)

        viewModel.onRemoveProduct(beer.id)

        assertEquals(listOf(soda.id), viewModel.uiState.value.products.map { it.id })
    }

    @Test
    fun confirmNeedsProductsAndDestination() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canConfirm)

        viewModel.onAddProduct(beer, 1)
        assertFalse(viewModel.uiState.value.canConfirm)

        viewModel.onDestinationSelected(OrderDestination.Room(204))
        assertTrue(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun savingResetsTheOrderButKeepsTheCatalog() {
        val viewModel = viewModel()
        viewModel.onAddProduct(beer, 2)
        viewModel.onDestinationSelected(OrderDestination.Table(3))

        viewModel.onConfirm()

        val saved = orders.saved.single()
        assertEquals(OrderDestination.Table(3), saved.destination)
        assertEquals(7_000, saved.totalAmount)
        val state = viewModel.uiState.value
        assertEquals(SaveResult.Saved, state.saveResult)
        assertTrue(state.products.isEmpty())
        assertNull(state.destination)
        assertEquals(listOf(beer, soda), state.catalog)

        viewModel.onResultDismissed()
        assertNull(viewModel.uiState.value.saveResult)
    }

    @Test
    fun failedSaveKeepsTheOrderAndRetriesWithTheSameId() {
        orders.result = Result.failure(OrderError.Timeout)
        val viewModel = viewModel()
        viewModel.onAddProduct(beer, 1)
        viewModel.onDestinationSelected(OrderDestination.Other)

        viewModel.onConfirm()

        val state = viewModel.uiState.value
        assertEquals(SaveResult.Failed(SaveFailure.Timeout), state.saveResult)
        assertFalse(state.isSaving)
        assertEquals(1, state.products.size)

        orders.result = Result.success(Unit)
        viewModel.onConfirm()

        assertEquals(2, orders.attempts.size)
        assertEquals(orders.attempts[0].id, orders.attempts[1].id)
    }

    @Test
    fun nextOrderGetsANewId() {
        val viewModel = viewModel()
        repeat(2) {
            viewModel.onAddProduct(beer, 1)
            viewModel.onDestinationSelected(OrderDestination.Other)
            viewModel.onConfirm()
        }

        assertNotEquals(orders.saved[0].id, orders.saved[1].id)
    }

    @Test
    fun reportsMissingSession() {
        val viewModel = AddOrderViewModel(
            GetProductsUseCase(catalog),
            SaveOrderUseCase(FakeAuthRepository(userId = null), orders),
        )
        viewModel.onAddProduct(beer, 1)
        viewModel.onDestinationSelected(OrderDestination.Other)

        viewModel.onConfirm()

        assertEquals(SaveResult.Failed(SaveFailure.NoSession), viewModel.uiState.value.saveResult)
    }
}
