package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveDirection
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveProductUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeCategoryRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InventoryUseCasesTest {
    private val snacks = Category(id = "snacks", name = "Snacks", position = 1)
    private val drinks = Category(id = "drinks", name = "Bebidas", position = 0)
    private val empty = Category(id = "pool", name = "Piscina", position = 2)
    private val beer = InventoryProduct(id = "beer", name = "cerveza Aguila", unitPrice = 3_500, categoryId = "drinks")
    private val water = InventoryProduct(id = "water", name = "Agua", unitPrice = 2_000, categoryId = "drinks")
    private val chips = InventoryProduct(id = "chips", name = "Papas", unitPrice = 2_500, categoryId = "snacks")
    private val oldJuice = InventoryProduct(id = "juice", name = "Jugo", unitPrice = 4_000, categoryId = "drinks", active = false)

    private val categories = FakeCategoryRepository(snacks, drinks, empty)
    private val products = FakeProductRepository(beer, water, chips, oldJuice)

    private fun categoryNames() = categories.categories.value.sortedBy { it.position }.map { it.name }

    // ---- RF-ORD-07: what can be ordered

    @Test
    fun theCatalogIsGroupedByCategoryInTheOrderOfTheAdministrator() = runTest {
        val catalog = GetProductsUseCase(products, categories)().getOrThrow()

        assertEquals(listOf("Bebidas", "Snacks"), catalog.map { it.categoryName })
        assertEquals(listOf("Agua", "cerveza Aguila"), catalog.first().products.map { it.name })
    }

    @Test
    fun inactiveProductsAndEmptyCategoriesAreNotOffered() = runTest {
        val catalog = GetProductsUseCase(products, categories)().getOrThrow()

        assertTrue(catalog.none { group -> group.products.any { it.id == "juice" } })
        assertTrue(catalog.none { it.categoryName == "Piscina" })
    }

    @Test
    fun aCatalogProductIsReadyToBeAddedToAnOrder() = runTest {
        val offered = GetProductsUseCase(products, categories)().getOrThrow().last().products.single()

        assertEquals(Product(id = "chips", name = "Papas", unitPrice = 2_500, quantity = 1), offered)
    }

    @Test
    fun aProductWhoseCategoryIsGoneIsStillOffered() = runTest {
        categories.categories.value = listOf(drinks)

        val catalog = GetProductsUseCase(products, categories)().getOrThrow()

        assertEquals(CatalogGroup(null, listOf(Product(id = "chips", name = "Papas", unitPrice = 2_500))), catalog.last())
    }

    @Test
    fun anInventoryThatCannotBeReadIsAFailure() = runTest {
        products.readFailure = IllegalStateException("permission denied")

        assertTrue(GetProductsUseCase(products, categories)().isFailure)
    }

    // ---- RF-INV-01

    @Test
    fun theInventoryShowsEveryProductAlsoTheInactiveOnes() = runTest {
        val inventory = ObserveInventoryUseCase(products, categories)().first()

        assertEquals(listOf(drinks, snacks, empty), inventory.categories)
        assertEquals(listOf("Agua", "cerveza Aguila", "Jugo"), inventory.productsOf(drinks).map { it.name })
        assertTrue(inventory.productsOf(empty).isEmpty())
    }

    // ---- RF-INV-02 and RF-INV-03

    @Test
    fun createsAProductWithANewId() = runTest {
        val result = SaveProductUseCase(products)(productId = null, name = "  Corona  ", unitPrice = 6_000, categoryId = "drinks")

        assertTrue(result.isSuccess)
        val created = products.products.value.last()
        assertEquals("Corona" to 6_000L, created.name to created.unitPrice)
        assertTrue(created.active)
        assertNotEquals("", created.id)
    }

    @Test
    fun editingKeepsTheIdSoTheProductIsNotDuplicated() = runTest {
        SaveProductUseCase(products)(productId = "beer", name = "Cerveza Aguila Light", unitPrice = 4_000, categoryId = "drinks")

        val edited = products.products.value.single { it.id == "beer" }
        assertEquals("Cerveza Aguila Light" to 4_000L, edited.name to edited.unitPrice)
        assertEquals(4, products.products.value.size)
    }

    @Test
    fun deactivatingKeepsTheProductInTheInventory() = runTest {
        SaveProductUseCase(products)(productId = "beer", name = beer.name, unitPrice = beer.unitPrice, categoryId = "drinks", active = false)

        assertEquals(false, products.products.value.single { it.id == "beer" }.active)
    }

    @Test
    fun aProductNeedsNamePriceAndCategory() = runTest {
        val save = SaveProductUseCase(products)

        assertEquals(InventoryError.BlankName, save(null, "   ", 1_000, "drinks").exceptionOrNull())
        assertEquals(InventoryError.InvalidPrice, save(null, "Corona", 0, "drinks").exceptionOrNull())
        assertEquals(InventoryError.InvalidPrice, save(null, "Corona", null, "drinks").exceptionOrNull())
        assertEquals(InventoryError.MissingCategory, save(null, "Corona", 1_000, null).exceptionOrNull())
        assertEquals(4, products.products.value.size)
    }

    // ---- RF-INV-04

    @Test
    fun aNewCategoryGoesToTheEnd() = runTest {
        SaveCategoryUseCase(categories)(categoryId = null, name = " Bar ")

        assertEquals(listOf("Bebidas", "Snacks", "Piscina", "Bar"), categoryNames())
    }

    @Test
    fun theFirstCategoryStartsTheList() = runTest {
        val emptyInventory = FakeCategoryRepository()

        SaveCategoryUseCase(emptyInventory)(categoryId = null, name = "Bar")

        assertEquals(0, emptyInventory.categories.value.single().position)
    }

    @Test
    fun renamingKeepsThePlaceAndTheProductsOfTheCategory() = runTest {
        SaveCategoryUseCase(categories)(categoryId = "drinks", name = "Bebidas frías")

        assertEquals(listOf("Bebidas frías", "Snacks", "Piscina"), categoryNames())
        assertEquals("drinks", categories.categories.value.single { it.name == "Bebidas frías" }.id)
    }

    @Test
    fun categoryNamesAreRequiredAndNotRepeated() = runTest {
        val save = SaveCategoryUseCase(categories)

        assertEquals(InventoryError.BlankName, save(null, "  ").exceptionOrNull())
        assertEquals(InventoryError.DuplicateCategory, save(null, "snacks").exceptionOrNull())
        assertEquals(InventoryError.DuplicateCategory, save("drinks", "Snacks").exceptionOrNull())
        assertTrue(save("snacks", "SNACKS").isSuccess)
        assertEquals(InventoryError.NotFound, save("gone", "Postres").exceptionOrNull())
    }

    @Test
    fun movingACategoryChangesTheOrder() = runTest {
        val move = MoveCategoryUseCase(categories)

        move("pool", MoveDirection.UP)
        assertEquals(listOf("Bebidas", "Piscina", "Snacks"), categoryNames())

        move("drinks", MoveDirection.DOWN)
        assertEquals(listOf("Piscina", "Bebidas", "Snacks"), categoryNames())
    }

    @Test
    fun theFirstCategoryCannotGoUpNorTheLastDown() = runTest {
        val move = MoveCategoryUseCase(categories)

        assertTrue(move("drinks", MoveDirection.UP).isSuccess)
        assertTrue(move("pool", MoveDirection.DOWN).isSuccess)

        assertEquals(listOf("Bebidas", "Snacks", "Piscina"), categoryNames())
    }

    @Test
    fun aCategoryWithoutProductsCanBeDeleted() = runTest {
        val result = DeleteCategoryUseCase(categories, products)("pool")

        assertTrue(result.isSuccess)
        assertEquals(listOf("Bebidas", "Snacks"), categoryNames())
    }

    @Test
    fun aCategoryWithProductsIsNotDeletedEvenIfTheyAreInactive() = runTest {
        products.products.value = listOf(oldJuice)

        val result = DeleteCategoryUseCase(categories, products)("drinks")

        assertEquals(InventoryError.CategoryInUse, result.exceptionOrNull())
        assertEquals(3, categories.categories.value.size)
    }
}
