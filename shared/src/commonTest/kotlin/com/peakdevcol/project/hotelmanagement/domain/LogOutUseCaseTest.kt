package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutOutcome
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePendingAdditionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogOutUseCaseTest {
    private val auth = FakeAuthRepository(userId = "uid-1")
    private val orders = FakeOrderRepository()
    private val pendingAdditions = FakePendingAdditionRepository()
    private val logOut = LogOutUseCase(auth, orders, pendingAdditions)

    @Test
    fun logsOutWhenEveryOrderWasSent() = runTest {
        assertEquals(LogOutOutcome.LOGGED_OUT, logOut())
        assertNull(auth.currentUserId())
    }

    @Test
    fun keepsTheSessionWhileOrdersWaitToBeSent() = runTest {
        orders.pendingOrders.value = mapOf("uid-1" to 2)

        assertEquals(LogOutOutcome.BLOCKED_BY_PENDING_ORDERS, logOut())
        assertEquals("uid-1", auth.currentUserId())
    }

    @Test
    fun thePendingOrdersOfAnotherEmployeeDoNotBlock() = runTest {
        orders.pendingOrders.value = mapOf("uid-2" to 1)

        assertEquals(LogOutOutcome.LOGGED_OUT, logOut())
    }

    @Test
    fun anAccountThatCannotReadItsOrdersCanStillLogOut() = runTest {
        orders.pendingOrdersFailure = IllegalStateException("permission denied")

        assertEquals(LogOutOutcome.LOGGED_OUT, logOut())
        assertNull(auth.currentUserId())
    }
}
