package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.matchesSearch
import com.peakdevcol.project.hotelmanagement.domain.model.matching
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderSearchTest {
    private fun listing(destination: OrderDestination, employeeName: String? = "Andrés Gómez") =
        OrderListing(order("o", destination = destination), employeeName, isMine = false)

    @Test
    fun digitsFindTheNumberOfTheDestinationFromItsStart() {
        assertTrue(listing(OrderDestination.Room(1204)).matchesSearch("12"))
        assertTrue(listing(OrderDestination.Table(4)).matchesSearch(" 4 "))
        assertFalse(listing(OrderDestination.Room(1204)).matchesSearch("04"))
        assertFalse(listing(OrderDestination.Other).matchesSearch("1"))
    }

    @Test
    fun wordsFindWhoTookTheOrderWithoutMindingAccents() {
        assertTrue(listing(OrderDestination.Table(4)).matchesSearch("andres"))
        assertTrue(listing(OrderDestination.Table(4)).matchesSearch("GÓMEZ"))
        assertFalse(listing(OrderDestination.Table(4)).matchesSearch("laura"))
        assertFalse(listing(OrderDestination.Table(4), employeeName = null).matchesSearch("andres"))
    }

    @Test
    fun aBlankSearchFindsEverything() {
        assertTrue(listing(OrderDestination.Other, employeeName = null).matchesSearch("   "))
    }

    @Test
    fun theCatalogKeepsOnlyTheGroupsWithMatches() {
        val catalog = listOf(
            CatalogGroup("Bebidas", listOf(Product("1", "Limonada de coco", 9_500), Product("2", "Café", 3_000))),
            CatalogGroup("Cocina", listOf(Product("3", "Hamburguesa", 32_000))),
        )

        val found = catalog.matching("cafe")

        assertEquals(listOf("Bebidas"), found.map { it.categoryName })
        assertEquals(listOf("Café"), found.single().products.map { it.name })
        assertEquals(catalog, catalog.matching(""))
    }
}
