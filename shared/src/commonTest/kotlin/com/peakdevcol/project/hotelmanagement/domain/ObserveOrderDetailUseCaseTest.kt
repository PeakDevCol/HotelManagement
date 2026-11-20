package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ObserveOrderDetailUseCaseTest {
    private val orders = FakeOrderRepository()
    private val users = FakeUserRepository(
        employee("uid-1", "Ana Gómez"),
        employee("admin", "Juan Felipe", type = UserType.ADMIN),
    )

    private fun observeAs(userId: String?) = ObserveOrderDetailUseCase(FakeAuthRepository(userId), orders, users)

    @Test
    fun showsTheOrderWithWhoTookIt() = runTest {
        val saved = order("a")
        orders.openOrders.value = listOf(saved)

        val detail = observeAs("uid-1")("a").first()

        assertEquals(saved, detail?.order)
        assertEquals("Ana Gómez", detail?.employeeName)
    }

    @Test
    fun theHistoryStartsWithTheCreationOfTheOrder() = runTest {
        val saved = order("a")
        orders.openOrders.value = listOf(saved)

        val history = observeAs("uid-1")("a").first()?.history

        assertEquals(listOf(OrderEvent(OrderEventType.CREATED, "uid-1", "Ana Gómez", saved.createdAt)), history)
    }

    @Test
    fun theHistoryContinuesWithEveryChangeAndWhoMadeIt() = runTest {
        orders.openOrders.value = listOf(order("a", state = OrderState.PAID))
        val closedAt = Instant.parse("2026-11-15T01:00:00Z")
        val paidAt = Instant.parse("2026-11-15T01:05:00Z")
        orders.events.value = mapOf(
            "a" to listOf(
                OrderEvent(OrderEventType.CLOSED, employeeId = "uid-1", at = closedAt),
                OrderEvent(
                    OrderEventType.PAID,
                    employeeId = "admin",
                    at = paidAt,
                    paymentMethod = PaymentMethod.CASH,
                    tipAmount = 2_000,
                ),
            ),
        )

        val history = observeAs("uid-1")("a").first()!!.history

        assertEquals(
            listOf(OrderEventType.CREATED, OrderEventType.CLOSED, OrderEventType.PAID),
            history.map { it.type },
        )
        assertEquals(listOf("Ana Gómez", "Ana Gómez", "Juan Felipe"), history.map { it.employeeName })
        assertEquals(2_000, history.last().tipAmount)
    }

    @Test
    fun offersTheActionsOfTheStateAndTheRole() = runTest {
        orders.openOrders.value = listOf(order("a"))

        assertEquals(listOf(OrderAction.CLOSE, OrderAction.PAY), observeAs("uid-1")("a").first()?.availableActions)
        assertEquals(listOf(OrderAction.CLOSE, OrderAction.PAY, OrderAction.CANCEL), observeAs("admin")("a").first()?.availableActions)
    }

    @Test
    fun offersNothingWhenTheEmployeeHasNoProfile() = runTest {
        orders.openOrders.value = listOf(order("a"))

        assertTrue(observeAs("unknown")("a").first()!!.availableActions.isEmpty())
    }

    @Test
    fun anOrderThatDoesNotExistHasNoDetail() = runTest {
        assertNull(observeAs("uid-1")("missing").first())
    }
}
