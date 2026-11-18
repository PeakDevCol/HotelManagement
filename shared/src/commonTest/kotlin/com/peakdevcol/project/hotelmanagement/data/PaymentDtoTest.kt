package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.PaymentDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toPayment
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toPaymentDto
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.fakes.beer
import com.peakdevcol.project.hotelmanagement.fakes.burger
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PaymentDtoTest {
    private val orderDto = Order.create("uid-1", listOf(beer, burger), OrderDestination.Room(204), id = "o-1").toDto()
    private val pay = OrderStateChange.Pay(employeeId = "uid-2", method = PaymentMethod.CASH, tipAmount = 1_000)

    @Test
    fun thePaymentCopiesWhatTheRulesCompareWithTheOrder() {
        val dto = pay.toPaymentDto("o-1", orderDto, orderAmount = 15_400)

        assertEquals(orderDto.products, dto.products)
        assertEquals("room" to 204, dto.typeOrder to dto.typeOrderNumber)
        assertEquals(1_900, dto.taxAmount)
        assertEquals(15_400, dto.amount)
        assertEquals(1_000, dto.tipAmount)
        assertEquals("cash", dto.method)
        assertEquals("order" to "o-1", dto.source to dto.sourceId)
        assertEquals("uid-2", dto.receivedBy)
        assertEquals(Timestamp.ServerTimestamp, dto.receivedAt)
    }

    @Test
    fun aStoredPaymentReadsBackWithItsDestinationAndLines() {
        val stored = pay.toPaymentDto("o-1", orderDto, orderAmount = 15_400).copy(receivedAt = Timestamp(1_763_400_000, 0))

        val payment = stored.toPayment("order_o-1")!!

        assertEquals(OrderDestination.Room(204), payment.destination)
        assertEquals(listOf("beer", "burger"), payment.lines.map { it.id })
        assertEquals(Instant.fromEpochSeconds(1_763_400_000), payment.receivedAt)
        assertEquals("o-1", payment.sourceId)
    }

    @Test
    fun aPaymentFromBeforeM5HasNoDestinationNorLines() {
        val old = PaymentDto(
            amount = 7_000,
            tipAmount = 0,
            method = "transfer",
            source = "order",
            sourceId = "o-2",
            receivedBy = "uid-1",
            receivedAt = Timestamp(1_763_400_000, 0),
        )

        val payment = old.toPayment("order_o-2")!!

        assertNull(payment.destination)
        assertEquals(emptyList(), payment.lines)
        assertEquals(0, payment.taxAmount)
    }

    @Test
    fun anUnknownMethodIsLeftOut() {
        val unknown = PaymentDto(
            amount = 7_000,
            tipAmount = 0,
            method = "card",
            source = "order",
            sourceId = "o-3",
            receivedBy = "uid-1",
            receivedAt = Timestamp(1_763_400_000, 0),
        )

        assertNull(unknown.toPayment("order_o-3"))
    }
}
