package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenOrdersUseCase
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

class ObserveOpenOrdersUseCaseTest {
    private val orders = FakeOrderRepository()
    private val users = FakeUserRepository(employee("uid-1", "Ana Gómez"), employee("uid-2", "Luis Pérez"))
    private val observeOpenOrders = ObserveOpenOrdersUseCase(FakeAuthRepository(userId = "uid-1"), orders, users)

    @Test
    fun theOldestOrderGoesFirst() = runTest {
        orders.openOrders.value = listOf(
            order("late", createdAt = "2026-11-15T03:00:00Z"),
            order("early", createdAt = "2026-11-15T01:00:00Z"),
            order("middle", createdAt = "2026-11-15T02:00:00Z"),
        )

        assertEquals(listOf("early", "middle", "late"), observeOpenOrders().first().map { it.order.id })
    }

    @Test
    fun anOrderWithoutDateYetGoesLast() = runTest {
        orders.openOrders.value = listOf(order("new", createdAt = null), order("old"))

        assertEquals(listOf("old", "new"), observeOpenOrders().first().map { it.order.id })
    }

    @Test
    fun tellsWhoTookEachOrderAndWhichAreMine() = runTest {
        orders.openOrders.value = listOf(
            order("a", employeeId = "uid-1", createdAt = "2026-11-15T01:00:00Z"),
            order("b", employeeId = "uid-2", createdAt = "2026-11-15T02:00:00Z"),
        )

        val (mine, others) = observeOpenOrders().first()

        assertEquals("Ana Gómez" to true, mine.employeeName to mine.isMine)
        assertEquals("Luis Pérez" to false, others.employeeName to others.isMine)
    }

    @Test
    fun anOrderOfAnEmployeeWithoutProfileHasNoName() = runTest {
        orders.openOrders.value = listOf(order("a", employeeId = "deleted"))

        assertNull(observeOpenOrders().first().single().employeeName)
    }
}
