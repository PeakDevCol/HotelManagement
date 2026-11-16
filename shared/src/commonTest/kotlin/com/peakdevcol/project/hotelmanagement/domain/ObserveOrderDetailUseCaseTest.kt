package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveOrderDetailUseCaseTest {
    private val orders = FakeOrderRepository()
    private val observeOrderDetail = ObserveOrderDetailUseCase(orders, FakeUserRepository(employee("uid-1", "Ana Gómez")))

    @Test
    fun showsTheOrderWithWhoTookIt() = runTest {
        val saved = order("a")
        orders.openOrders.value = listOf(saved)

        val detail = observeOrderDetail("a").first()

        assertEquals(saved, detail?.order)
        assertEquals("Ana Gómez", detail?.employeeName)
    }

    @Test
    fun theHistoryStartsWithTheCreationOfTheOrder() = runTest {
        val saved = order("a")
        orders.openOrders.value = listOf(saved)

        val history = observeOrderDetail("a").first()?.history

        assertEquals(listOf(OrderEvent(OrderEventType.CREATED, "Ana Gómez", saved.createdAt)), history)
    }

    @Test
    fun anOrderThatDoesNotExistHasNoDetail() = runTest {
        assertNull(observeOrderDetail("missing").first())
    }
}
