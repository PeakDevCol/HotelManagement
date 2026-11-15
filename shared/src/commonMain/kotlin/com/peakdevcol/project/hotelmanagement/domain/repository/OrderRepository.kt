package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    /**
     * Saves the order on the device and tells whether the server confirmed it right away.
     * It fails only when the order could not be saved at all.
     */
    suspend fun saveOrder(order: Order): Result<OrderDelivery>

    /** Emits how many in-progress orders of the employee are still waiting to be sent. */
    fun observePendingOrderCount(employeeId: String): Flow<Int>
}
