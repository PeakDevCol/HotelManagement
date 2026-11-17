package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveDirection
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveProductUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeCategoryRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeProductRepository
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesEffect
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesIntent
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoryDialog
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryEffect
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryIntent
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryViewModel
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormEffect
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormIntent
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InventoryViewModelsTest : MainDispatcherTest() {
    private val drinks = Category(id = "drinks", name = "Bebidas", position = 0)
    private val snacks = Category(id = "snacks", name = "Snacks", position = 1)
    private val beer = InventoryProduct(id = "beer", name = "Cerveza Aguila", unitPrice = 3_500, categoryId = "drinks", taxId = "iva")

    private val categories = FakeCategoryRepository(drinks, snacks)
    private val products = FakeProductRepository(beer)
    private val observeInventory = ObserveInventoryUseCase(products, categories)

    // ---- inventory list

    private fun inventory() = InventoryViewModel(observeInventory)

    @Test
    fun showsTheInventoryAndFollowsItsChanges() {
        val viewModel = inventory()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(listOf(beer), viewModel.state.value.inventory.products)

        products.products.value += beer.copy(id = "water", name = "Agua")

        assertEquals(2, viewModel.state.value.inventory.products.size)
    }

    @Test
    fun anEmptyInventoryAsksForACategoryFirst() = runTest {
        categories.categories.value = emptyList()
        products.products.value = emptyList()
        val viewModel = inventory()

        viewModel.onIntent(InventoryIntent.NewProduct)
        viewModel.onIntent(InventoryIntent.ManageCategories)

        assertTrue(viewModel.state.value.needsCategories)
        assertFalse(viewModel.state.value.canCreateProducts)
        assertEquals(InventoryEffect.NavigateToCategories, viewModel.effects.first())
    }

    @Test
    fun opensTheFormToCreateOrEditAProduct() = runTest {
        val viewModel = inventory()

        viewModel.onIntent(InventoryIntent.NewProduct)
        assertEquals(InventoryEffect.NavigateToProduct(productId = null), viewModel.effects.first())

        viewModel.onIntent(InventoryIntent.OpenProduct("beer"))
        assertEquals(InventoryEffect.NavigateToProduct("beer"), viewModel.effects.first())
    }

    @Test
    fun showsAnErrorAndRetries() {
        products.readFailure = IllegalStateException("permission denied")
        val viewModel = inventory()
        assertTrue(viewModel.state.value.loadFailed)

        products.readFailure = null
        viewModel.onIntent(InventoryIntent.Retry)

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(listOf(beer), viewModel.state.value.inventory.products)
    }

    // ---- product form

    private fun form(productId: String?) = ProductFormViewModel(productId, observeInventory, SaveProductUseCase(products))

    @Test
    fun aNewProductStartsEmptyAndActive() {
        val state = form(productId = null).state.value

        assertTrue(state.isNew)
        assertEquals("", state.name)
        assertTrue(state.active)
        assertEquals(listOf(drinks, snacks), state.categories)
    }

    @Test
    fun creatingAProductSavesItAndClosesTheForm() = runTest {
        val viewModel = form(productId = null)
        viewModel.onIntent(ProductFormIntent.NameChange("Papas BBQ"))
        viewModel.onIntent(ProductFormIntent.PriceChange("2.000 pesos"))
        viewModel.onIntent(ProductFormIntent.CategorySelect("snacks"))

        viewModel.onIntent(ProductFormIntent.Save)

        val created = products.products.value.single { it.name == "Papas BBQ" }
        assertEquals(2_000L to "snacks", created.unitPrice to created.categoryId)
        assertEquals(ProductFormEffect.Saved, viewModel.effects.first())
    }

    @Test
    fun editingShowsTheProductAndKeepsWhatTheFormDoesNotEdit() = runTest {
        val viewModel = form(productId = "beer")
        assertEquals("Cerveza Aguila" to "3500", viewModel.state.value.name to viewModel.state.value.priceText)
        assertFalse(viewModel.state.value.isNew)

        viewModel.onIntent(ProductFormIntent.PriceChange("4000"))
        viewModel.onIntent(ProductFormIntent.ActiveChange(false))
        viewModel.onIntent(ProductFormIntent.Save)

        assertEquals(beer.copy(unitPrice = 4_000, active = false), products.products.value.single())
    }

    @Test
    fun aWrongFieldIsMarkedAndNothingIsSaved() {
        val viewModel = form(productId = null)
        viewModel.onIntent(ProductFormIntent.NameChange("Papas"))

        viewModel.onIntent(ProductFormIntent.Save)
        assertEquals(InventoryError.InvalidPrice, viewModel.state.value.fieldError)

        viewModel.onIntent(ProductFormIntent.PriceChange("2000"))
        assertNull(viewModel.state.value.fieldError)

        viewModel.onIntent(ProductFormIntent.Save)
        assertEquals(InventoryError.MissingCategory, viewModel.state.value.fieldError)
        assertEquals(listOf(beer), products.products.value)
    }

    @Test
    fun aRejectedSaveKeepsTheFormOpenAndWarns() = runTest {
        products.saveFailure = InventoryError.NotAllowed
        val viewModel = form(productId = "beer")

        viewModel.onIntent(ProductFormIntent.Save)

        assertEquals(ProductFormEffect.SaveFailed(InventoryError.NotAllowed), viewModel.effects.first())
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun tellsWhenTheProductToEditDoesNotExist() {
        assertTrue(form(productId = "gone").state.value.notFound)
    }

    // ---- categories

    private fun categoriesScreen() = CategoriesViewModel(
        observeInventory,
        SaveCategoryUseCase(categories),
        MoveCategoryUseCase(categories),
        DeleteCategoryUseCase(categories, products),
    )

    private fun CategoriesViewModel.names() = state.value.categories.map { it.name }

    @Test
    fun createsACategory() {
        val viewModel = categoriesScreen()
        viewModel.onIntent(CategoriesIntent.AddClick)
        viewModel.onIntent(CategoriesIntent.NameChange("Bar"))

        viewModel.onIntent(CategoriesIntent.ConfirmDialog)

        assertEquals(listOf("Bebidas", "Snacks", "Bar"), viewModel.names())
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun renamesACategory() {
        val viewModel = categoriesScreen()
        viewModel.onIntent(CategoriesIntent.RenameClick(snacks))
        assertEquals("Snacks", assertIs<CategoryDialog.Name>(viewModel.state.value.dialog).text)

        viewModel.onIntent(CategoriesIntent.NameChange("Pasabocas"))
        viewModel.onIntent(CategoriesIntent.ConfirmDialog)

        assertEquals(listOf("Bebidas", "Pasabocas"), viewModel.names())
    }

    @Test
    fun aRepeatedNameKeepsTheDialogOpenWithItsError() {
        val viewModel = categoriesScreen()
        viewModel.onIntent(CategoriesIntent.AddClick)
        viewModel.onIntent(CategoriesIntent.NameChange("bebidas"))

        viewModel.onIntent(CategoriesIntent.ConfirmDialog)

        val dialog = assertIs<CategoryDialog.Name>(viewModel.state.value.dialog)
        assertEquals(InventoryError.DuplicateCategory, dialog.error)
        assertEquals(2, viewModel.names().size)

        viewModel.onIntent(CategoriesIntent.NameChange("Bar"))
        assertNull(assertIs<CategoryDialog.Name>(viewModel.state.value.dialog).error)
    }

    @Test
    fun movesACategory() {
        val viewModel = categoriesScreen()

        viewModel.onIntent(CategoriesIntent.Move("snacks", MoveDirection.UP))

        assertEquals(listOf("Snacks", "Bebidas"), viewModel.names())
    }

    @Test
    fun deletesAnEmptyCategoryAfterConfirming() {
        val viewModel = categoriesScreen()
        viewModel.onIntent(CategoriesIntent.DeleteClick(snacks))
        assertEquals(CategoryDialog.Delete(snacks), viewModel.state.value.dialog)

        viewModel.onIntent(CategoriesIntent.ConfirmDialog)

        assertEquals(listOf("Bebidas"), viewModel.names())
    }

    @Test
    fun aCategoryWithProductsIsNotDeletedAndTheReasonIsShown() = runTest {
        val viewModel = categoriesScreen()
        viewModel.onIntent(CategoriesIntent.DeleteClick(drinks))

        viewModel.onIntent(CategoriesIntent.ConfirmDialog)

        assertEquals(CategoriesEffect.Failed(InventoryError.CategoryInUse), viewModel.effects.first())
        assertEquals(listOf("Bebidas", "Snacks"), viewModel.names())
    }

    @Test
    fun dismissingADialogChangesNothing() {
        val viewModel = categoriesScreen()
        viewModel.onIntent(CategoriesIntent.DeleteClick(snacks))

        viewModel.onIntent(CategoriesIntent.DismissDialog)

        assertNull(viewModel.state.value.dialog)
        assertEquals(2, viewModel.names().size)
    }
}
