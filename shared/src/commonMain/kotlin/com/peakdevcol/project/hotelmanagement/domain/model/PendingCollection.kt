package com.peakdevcol.project.hotelmanagement.domain.model

/** RF-BAL-04: what is still to be collected right now, whatever the period of the balance. */
data class PendingCollection(
    val inProgressCount: Int = 0,
    val inProgressAmount: Long = 0,
    val closedCount: Int = 0,
    val closedAmount: Long = 0,
) {
    val totalAmount: Long get() = inProgressAmount + closedAmount

    companion object {
        /** The totals come from the lines, as everywhere else in the app (V6). */
        fun of(orders: List<Order>): PendingCollection {
            val inProgress = orders.filter { it.state == OrderState.IN_PROGRESS }
            val closed = orders.filter { it.state == OrderState.CLOSED }
            return PendingCollection(
                inProgressCount = inProgress.size,
                inProgressAmount = inProgress.sumOf { it.totalAmount },
                closedCount = closed.size,
                closedAmount = closed.sumOf { it.totalAmount },
            )
        }
    }
}
