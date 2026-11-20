package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ChargeOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.CloseOrderUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReopenOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderStateUseCasesTest {
    private val auth = FakeAuthRepository(userId = "uid-1")
    private val noSession = FakeAuthRepository(userId = null)
    private val users = FakeUserRepository(
        employee("uid-1", "Ana Gómez"),
        employee("admin", "Juan Felipe", type = UserType.ADMIN),
    )
    private val orders = FakeOrderRepository().apply {
        openOrders.value = listOf(order("open"), order("closed", state = OrderState.CLOSED))
    }

    private fun stateOf(orderId: String) = orders.openOrders.value.first { it.id == orderId }.state

    @Test
    fun closingLeavesTheOrderClosedByTheEmployee() = runTest {
        val result = CloseOrderUseCase(auth, orders)("open")

        assertTrue(result.isSuccess)
        assertEquals(OrderState.CLOSED, stateOf("open"))
        assertEquals("open" to OrderStateChange.Close("uid-1"), orders.appliedChanges.single())
    }

    @Test
    fun anOrderAlreadyClosedByAnotherDeviceIsNotClosedAgain() = runTest {
        val result = CloseOrderUseCase(auth, orders)("closed")

        assertEquals(OrderActionError.StateChanged(OrderState.CLOSED), result.exceptionOrNull())
        assertTrue(orders.appliedChanges.isEmpty())
    }

    @Test
    fun reopeningPutsAClosedOrderBackInProgress() = runTest {
        val result = ReopenOrderUseCase(auth, orders)("closed")

        assertTrue(result.isSuccess)
        assertEquals(OrderState.IN_PROGRESS, stateOf("closed"))
    }

    private fun orderNamed(orderId: String) = orders.openOrders.value.first { it.id == orderId }

    @Test
    fun chargingRecordsTheMethodAndTheTip() = runTest {
        val result = ChargeOrderUseCase(auth, orders)(orderNamed("closed"), PaymentMethod.TRANSFER, tipAmount = 2_000)

        assertTrue(result.isSuccess)
        assertEquals(OrderState.PAID, stateOf("closed"))
        assertEquals(
            "closed" to OrderStateChange.Pay("uid-1", PaymentMethod.TRANSFER, tipAmount = 2_000),
            orders.appliedChanges.single(),
        )
    }

    @Test
    fun theTipIsOptional() = runTest {
        ChargeOrderUseCase(auth, orders)(orderNamed("closed"), PaymentMethod.CASH)

        assertEquals(0, (orders.appliedChanges.single().second as OrderStateChange.Pay).tipAmount)
    }

    @Test
    fun aNegativeTipIsRejectedBeforeReachingTheServer() = runTest {
        val result = ChargeOrderUseCase(auth, orders)(orderNamed("open"), PaymentMethod.CASH, tipAmount = -1)

        assertEquals(OrderActionError.InvalidTip, result.exceptionOrNull())
        assertEquals(OrderState.IN_PROGRESS, stateOf("open"))
        assertTrue(orders.appliedChanges.isEmpty())
    }

    @Test
    fun anOrderIsNeverPaidTwice() = runTest {
        val charge = ChargeOrderUseCase(auth, orders)
        val closed = orderNamed("closed")
        charge(closed, PaymentMethod.CASH)

        val second = charge(closed, PaymentMethod.CASH)

        assertEquals(OrderActionError.StateChanged(OrderState.PAID), second.exceptionOrNull())
        assertEquals(1, orders.appliedChanges.size)
    }

    // ---- Charging an order in progress (PRD, section 13, 2026-11-19)

    @Test
    fun anOrderInProgressIsClosedAndThenPaid() = runTest {
        val result = ChargeOrderUseCase(auth, orders)(orderNamed("open"), PaymentMethod.CASH, tipAmount = 500)

        assertTrue(result.isSuccess)
        assertEquals(OrderState.PAID, stateOf("open"))
        assertEquals(
            listOf(
                "open" to OrderStateChange.Close("uid-1"),
                "open" to OrderStateChange.Pay("uid-1", PaymentMethod.CASH, tipAmount = 500),
            ),
            orders.appliedChanges,
        )
    }

    @Test
    fun anOrderThatCouldNotBeClosedIsNotPaid() = runTest {
        val seenInProgress = orderNamed("closed").copy(state = OrderState.IN_PROGRESS)

        val result = ChargeOrderUseCase(auth, orders)(seenInProgress, PaymentMethod.CASH)

        assertEquals(OrderActionError.StateChanged(OrderState.CLOSED), result.exceptionOrNull())
        assertEquals(OrderState.CLOSED, stateOf("closed"))
        assertTrue(orders.appliedChanges.isEmpty())
    }

    @Test
    fun anAdministratorCancelsWithAReason() = runTest {
        val result = CancelOrderUseCase(FakeAuthRepository(userId = "admin"), users, orders)("open", "  El cliente se fue  ")

        assertTrue(result.isSuccess)
        assertEquals(OrderState.CANCELLED, stateOf("open"))
        assertEquals(
            "open" to OrderStateChange.Cancel("admin", "El cliente se fue"),
            orders.appliedChanges.single(),
        )
    }

    @Test
    fun cancellingNeedsAReason() = runTest {
        val result = CancelOrderUseCase(FakeAuthRepository(userId = "admin"), users, orders)("open", "   ")

        assertEquals(OrderActionError.MissingReason, result.exceptionOrNull())
        assertEquals(OrderState.IN_PROGRESS, stateOf("open"))
    }

    @Test
    fun anEmployeeWhoIsNotAnAdministratorCannotCancel() = runTest {
        val result = CancelOrderUseCase(auth, users, orders)("open", "Se equivocó de mesa")

        assertEquals(OrderActionError.NotAllowed, result.exceptionOrNull())
        assertEquals(OrderState.IN_PROGRESS, stateOf("open"))
    }

    @Test
    fun nothingChangesWithoutASession() = runTest {
        assertEquals(OrderActionError.NoSession, CloseOrderUseCase(noSession, orders)("open").exceptionOrNull())
        assertEquals(OrderActionError.NoSession, ReopenOrderUseCase(noSession, orders)("closed").exceptionOrNull())
        assertEquals(
            OrderActionError.NoSession,
            ChargeOrderUseCase(noSession, orders)(orderNamed("closed"), PaymentMethod.CASH).exceptionOrNull(),
        )
        assertEquals(OrderActionError.NoSession, CancelOrderUseCase(noSession, users, orders)("open", "x").exceptionOrNull())
        assertTrue(orders.appliedChanges.isEmpty())
    }

    @Test
    fun aMissingOrderIsReported() = runTest {
        assertEquals(OrderActionError.NotFound, CloseOrderUseCase(auth, orders)("missing").exceptionOrNull())
    }
}
