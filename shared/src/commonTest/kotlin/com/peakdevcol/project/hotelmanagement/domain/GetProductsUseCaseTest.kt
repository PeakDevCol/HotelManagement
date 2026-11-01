package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.data.catalog.StaticProductCatalog
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeProductCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetProductsUseCaseTest {
    @Test
    fun sortsProductsByNameIgnoringCase() = runTest {
        val catalog = FakeProductCatalog(
            Result.success(
                listOf(
                    Product(id = "1", name = "corona", unitPrice = 6_000),
                    Product(id = "2", name = "Aguila", unitPrice = 3_500),
                    Product(id = "3", name = "Bombombun", unitPrice = 1_000),
                ),
            ),
        )

        val names = GetProductsUseCase(catalog)().getOrThrow().map { it.name }

        assertEquals(listOf("Aguila", "Bombombun", "corona"), names)
    }

    @Test
    fun staticCatalogHasUniqueIdsAndPositivePrices() = runTest {
        val products = StaticProductCatalog().getProducts().getOrThrow()

        assertEquals(products.size, products.map { it.id }.toSet().size)
        assertTrue(products.all { it.unitPrice > 0 && it.quantity == 1 })
    }
}
