package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OrderTest {

    private val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 2)
    private val pool = Product(id = "7", name = "Piscina", unitPrice = 7_000, quantity = 1)

    @Test
    fun productTotalIsUnitPriceTimesQuantity() {
        assertEquals(7_000, beer.totalAmount)
    }

    @Test
    fun createdOrderIsConfirmedAndSumsAllProducts() {
        val order = Order.create("employee", listOf(beer, pool), OrderDestination.Room(12))

        assertEquals(OrderState.CONFIRM, order.state)
        assertEquals(14_000, order.totalAmount)
        assertEquals("employee", order.employeeId)
    }

    @Test
    fun everyCreatedOrderHasItsOwnId() {
        val first = Order.create("employee", listOf(beer), OrderDestination.Other)
        val second = Order.create("employee", listOf(beer), OrderDestination.Other)

        assertNotEquals(first.id, second.id)
    }
}
