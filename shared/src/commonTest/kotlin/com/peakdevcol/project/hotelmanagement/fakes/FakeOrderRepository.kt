package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

class FakeOrderRepository(
    var result: Result<Unit> = Result.success(Unit),
) : OrderRepository {
    /** Every order passed to [saveOrder], also the failed ones. */
    val attempts = mutableListOf<Order>()
    val saved = mutableListOf<Order>()

    override suspend fun saveOrder(order: Order): Result<Unit> {
        attempts += order
        if (result.isSuccess) saved += order
        return result
    }
}
