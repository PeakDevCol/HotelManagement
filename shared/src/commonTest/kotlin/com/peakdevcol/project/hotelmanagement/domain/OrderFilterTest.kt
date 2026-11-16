package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.OrderFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.model.OrderOwnerFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderFilterTest {
    private val mineInProgress = OrderListing(order("a"), "Ana", isMine = true)
    private val mineClosed = OrderListing(order("b", state = OrderState.CLOSED), "Ana", isMine = true)
    private val othersInProgress = OrderListing(order("c", employeeId = "uid-2"), "Luis", isMine = false)
    private val all = listOf(mineInProgress, mineClosed, othersInProgress)

    private fun idsMatching(filter: OrderFilter) = all.filter(filter::matches).map { it.order.id }

    @Test
    fun byDefaultShowsEveryOrder() {
        assertEquals(listOf("a", "b", "c"), idsMatching(OrderFilter()))
    }

    @Test
    fun myOrdersHidesTheOrdersOfOthers() {
        assertEquals(listOf("a", "b"), idsMatching(OrderFilter(owner = OrderOwnerFilter.MINE)))
    }

    @Test
    fun filtersByState() {
        assertEquals(listOf("b"), idsMatching(OrderFilter(state = OrderState.CLOSED)))
    }

    @Test
    fun bothFiltersApplyTogether() {
        val filter = OrderFilter(owner = OrderOwnerFilter.MINE, state = OrderState.IN_PROGRESS)

        assertEquals(listOf("a"), idsMatching(filter))
    }
}
