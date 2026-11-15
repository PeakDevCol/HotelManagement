package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeOrderRepository(
    var result: Result<OrderDelivery> = Result.success(OrderDelivery.SENT),
) : OrderRepository {
    /** Every order passed to [saveOrder], also the failed ones. */
    val attempts = mutableListOf<Order>()
    val saved = mutableListOf<Order>()

    /** Orders waiting to be sent, by employee. */
    val pendingOrders = MutableStateFlow(emptyMap<String, Int>())
    var pendingOrdersFailure: Throwable? = null

    override suspend fun saveOrder(order: Order): Result<OrderDelivery> {
        attempts += order
        if (result.isSuccess) saved += order
        return result
    }

    override fun observePendingOrderCount(employeeId: String): Flow<Int> =
        pendingOrdersFailure?.let { error -> flow { throw error } }
            ?: pendingOrders.map { it[employeeId] ?: 0 }
}
