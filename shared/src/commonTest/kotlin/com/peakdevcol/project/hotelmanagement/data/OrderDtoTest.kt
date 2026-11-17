package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toOrder
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class OrderDtoTest {
    private val beer = Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 2)

    private fun orderTo(destination: OrderDestination) = Order.create("uid-1", listOf(beer), destination).toDto()

    @Test
    fun statesUseTheCodesOfThePrd() {
        assertEquals(
            listOf("in_progress", "closed", "paid", "cancelled"),
            OrderState.entries.map { it.code() },
        )
    }

    @Test
    fun aNewOrderIsStoredInProgressWithTheServerDate() {
        val dto = orderTo(OrderDestination.Room(204))

        assertEquals("in_progress", dto.state)
        assertEquals(Timestamp.ServerTimestamp, dto.dateOrder)
        assertEquals("uid-1", dto.idEmployee)
        assertEquals(7_000, dto.totalPayAmount)
    }

    @Test
    fun roomsAndTablesKeepTheirNumber() {
        assertEquals("room" to 204, orderTo(OrderDestination.Room(204)).let { it.typeOrder to it.typeOrderNumber })
        assertEquals("table" to 3, orderTo(OrderDestination.Table(3)).let { it.typeOrder to it.typeOrderNumber })
    }

    @Test
    fun thePoolAndTheBarHaveAnOptionalNumber() {
        assertEquals("pool" to 12, orderTo(OrderDestination.Pool(12)).let { it.typeOrder to it.typeOrderNumber })
        assertEquals("bar", orderTo(OrderDestination.Bar()).typeOrder)
        assertNull(orderTo(OrderDestination.Bar()).typeOrderNumber)
    }

    @Test
    fun otherHasNoNumber() {
        val dto = orderTo(OrderDestination.Other)

        assertEquals("other", dto.typeOrder)
        assertNull(dto.typeOrderNumber)
    }

    @Test
    fun readsBackTheOrderItStored() {
        val saved = Order.create("uid-1", listOf(beer), OrderDestination.Pool(12), id = "order-1")
        val stored = saved.toDto().copy(dateOrder = Timestamp(seconds = 1_794_702_180, nanoseconds = 0))

        val read = stored.toOrder(isPendingToSend = true)

        assertEquals(
            saved.copy(createdAt = Instant.fromEpochSeconds(1_794_702_180), isPendingToSend = true),
            read,
        )
    }

    @Test
    fun anOrderWithAnUnknownStateOrDestinationIsNotValid() {
        val stored = orderTo(OrderDestination.Room(204))

        assertNull(stored.copy(state = "confirm").toOrder(isPendingToSend = false))
        assertNull(stored.copy(typeOrder = "lobby").toOrder(isPendingToSend = false))
        assertNull(stored.copy(typeOrderNumber = null).toOrder(isPendingToSend = false))
    }

    @Test
    fun everyLineOfANewOrderIsStoredWithItsOwnId() {
        val soda = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000)

        val lineIds = Order.create("uid-1", listOf(beer, soda), OrderDestination.Other).toDto().products.map { it.lineId }

        assertEquals(2, lineIds.filterNotNull().distinct().size)
    }

    @Test
    fun linesStoredWithoutIdGetAStableOneFromTheirPosition() {
        val legacy = orderTo(OrderDestination.Other).let { dto -> dto.copy(products = dto.products.map { it.copy(lineId = null) } + dto.products.map { it.copy(lineId = null) }) }

        val first = legacy.toOrder(isPendingToSend = false)!!.products.map { it.lineId }
        val second = legacy.toOrder(isPendingToSend = false)!!.products.map { it.lineId }

        assertEquals(listOf("line-0", "line-1"), first)
        assertEquals(first, second)
    }
}
