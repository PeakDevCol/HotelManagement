package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.actionsFor
import com.peakdevcol.project.hotelmanagement.domain.model.after
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderActionTest {
    @Test
    fun anOrderInProgressCanBeClosedOrCancelled() {
        assertEquals(OrderState.CLOSED, OrderState.IN_PROGRESS.after(OrderAction.CLOSE))
        assertEquals(OrderState.CANCELLED, OrderState.IN_PROGRESS.after(OrderAction.CANCEL))
        assertNull(OrderState.IN_PROGRESS.after(OrderAction.PAY))
        assertNull(OrderState.IN_PROGRESS.after(OrderAction.REOPEN))
    }

    @Test
    fun aClosedOrderCanBeReopenedPaidOrCancelled() {
        assertEquals(OrderState.IN_PROGRESS, OrderState.CLOSED.after(OrderAction.REOPEN))
        assertEquals(OrderState.PAID, OrderState.CLOSED.after(OrderAction.PAY))
        assertEquals(OrderState.CANCELLED, OrderState.CLOSED.after(OrderAction.CANCEL))
        assertNull(OrderState.CLOSED.after(OrderAction.CLOSE))
    }

    @Test
    fun paidAndCancelledOrdersNeverChange() {
        OrderAction.entries.forEach { action ->
            assertNull(OrderState.PAID.after(action))
            assertNull(OrderState.CANCELLED.after(action))
        }
    }

    @Test
    fun onlyAnAdministratorIsOfferedTheCancellation() {
        assertEquals(listOf(OrderAction.CLOSE, OrderAction.PAY), OrderState.IN_PROGRESS.actionsFor(UserType.BASIC))
        assertEquals(listOf(OrderAction.CLOSE, OrderAction.PAY), OrderState.IN_PROGRESS.actionsFor(UserType.RECEPTION))
        assertEquals(
            listOf(OrderAction.CLOSE, OrderAction.PAY, OrderAction.CANCEL),
            OrderState.IN_PROGRESS.actionsFor(UserType.ADMIN),
        )
        assertEquals(listOf(OrderAction.REOPEN, OrderAction.PAY), OrderState.CLOSED.actionsFor(UserType.BASIC))
        assertEquals(
            listOf(OrderAction.REOPEN, OrderAction.PAY, OrderAction.CANCEL),
            OrderState.CLOSED.actionsFor(UserType.ADMIN),
        )
    }

    @Test
    fun aPaidOrderOffersNothingEvenToAnAdministrator() {
        assertTrue(OrderState.PAID.actionsFor(UserType.ADMIN).isEmpty())
        assertTrue(OrderState.CANCELLED.actionsFor(UserType.ADMIN).isEmpty())
    }
}
