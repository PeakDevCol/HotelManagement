package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.MAX_PRODUCT_QUANTITY
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-ORD-01 and RF-ORD-02. */
class ProductSelectionTest {
    private val beer = Product(id = "beer", name = "Cerveza", unitPrice = 7_000)
    private val water = Product(id = "water", name = "Agua", unitPrice = 5_000)

    @Test
    fun choosingAProductAddsOneUnit() {
        val selection = ProductSelection().add(beer)

        assertEquals(1, selection.quantityOf("beer"))
        assertEquals(7_000, selection.totalAmount)
    }

    @Test
    fun choosingItAgainAddsAnotherUnitInTheSameLine() {
        val selection = ProductSelection().add(beer).add(water).add(beer)

        assertEquals(listOf("beer", "water"), selection.products.map { it.id })
        assertEquals(2, selection.quantityOf("beer"))
        assertEquals(3, selection.unitCount)
        assertEquals(19_000, selection.totalAmount)
    }

    @Test
    fun theQuantityStaysBetweenOneAndNinetyNine() {
        var selection = ProductSelection().add(beer)
        repeat(120) { selection = selection.increase("beer") }
        assertEquals(MAX_PRODUCT_QUANTITY, selection.quantityOf("beer"))

        repeat(120) { selection = selection.decrease("beer") }
        assertEquals(1, selection.quantityOf("beer"))
    }

    @Test
    fun removingTakesTheProductOut() {
        val selection = ProductSelection().add(beer).add(water).remove("beer")

        assertEquals(0, selection.quantityOf("beer"))
        assertEquals(listOf("water"), selection.products.map { it.id })
        assertTrue(ProductSelection().add(beer).remove("beer").isEmpty)
    }
}
