package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.OrderEventDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.orderPaymentId
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toEvent
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toEventDto
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class OrderEventDtoTest {
    @Test
    fun everyChangeIsStoredWithItsTypeAndWhoMadeIt() {
        assertEquals("closed" to "uid-1", OrderStateChange.Close("uid-1").toEventDto().let { it.type to it.by })
        assertEquals("reopened", OrderStateChange.Reopen("uid-1").toEventDto().type)
        assertEquals("paid", OrderStateChange.Pay("uid-1", PaymentMethod.CASH, 0).toEventDto().type)
        assertEquals("cancelled", OrderStateChange.Cancel("uid-1", "Duplicado").toEventDto().type)
    }

    @Test
    fun theServerDatesTheEvent() {
        assertEquals(Timestamp.ServerTimestamp, OrderStateChange.Close("uid-1").toEventDto().at)
    }

    @Test
    fun aPaymentEventKeepsTheMethodAndTheTip() {
        val dto = OrderStateChange.Pay("uid-1", PaymentMethod.TRANSFER, tipAmount = 2_000).toEventDto()

        assertEquals("transfer", dto.method)
        assertEquals(2_000, dto.tipAmount)
    }

    @Test
    fun aCancellationEventKeepsTheReason() {
        assertEquals("Duplicado", OrderStateChange.Cancel("admin", "Duplicado").toEventDto().reason)
    }

    @Test
    fun readsBackAStoredEvent() {
        val stored = OrderEventDto(
            type = "paid",
            by = "uid-1",
            at = Timestamp(seconds = 1_794_702_180, nanoseconds = 0),
            method = "cash",
            tipAmount = 500,
        )

        val event = stored.toEvent()

        assertEquals(OrderEventType.PAID, event?.type)
        assertEquals("uid-1", event?.employeeId)
        assertEquals(Instant.fromEpochSeconds(1_794_702_180), event?.at)
        assertEquals(PaymentMethod.CASH, event?.paymentMethod)
        assertEquals(500, event?.tipAmount)
    }

    @Test
    fun anEventOfAnUnknownTypeIsSkipped() {
        assertNull(OrderEventDto(type = "teleported", by = "uid-1").toEvent())
    }

    @Test
    fun anOrderHasOnlyOnePaymentId() {
        assertEquals("order_order-1", orderPaymentId("order-1"))
    }
}
