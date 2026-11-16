package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.after
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

    val openOrders = MutableStateFlow(emptyList<Order>())
    var openOrdersFailure: Throwable? = null

    override fun observeOpenOrders(): Flow<List<Order>> =
        openOrdersFailure?.let { error -> flow { throw error } } ?: openOrders

    override fun observeOrder(orderId: String): Flow<Order?> =
        openOrdersFailure?.let { error -> flow { throw error } }
            ?: openOrders.map { orders -> orders.firstOrNull { it.id == orderId } }

    val events = MutableStateFlow(emptyMap<String, List<OrderEvent>>())

    /** Every change that was applied, with the id of its order. */
    val appliedChanges = mutableListOf<Pair<String, OrderStateChange>>()
    var changeFailure: OrderActionError? = null

    override fun observeOrderEvents(orderId: String): Flow<List<OrderEvent>> =
        events.map { it[orderId].orEmpty() }

    /** Follows the same contract as Firestore: the change applies only from a state that allows it. */
    override suspend fun changeState(orderId: String, change: OrderStateChange): Result<Unit> {
        changeFailure?.let { return Result.failure(it) }
        val order = openOrders.value.firstOrNull { it.id == orderId }
            ?: return Result.failure(OrderActionError.NotFound)
        val newState = order.state.after(change.action)
            ?: return Result.failure(OrderActionError.StateChanged(order.state))
        openOrders.value = openOrders.value.map { if (it.id == orderId) it.copy(state = newState) else it }
        appliedChanges += orderId to change
        return Result.success(Unit)
    }

    var addProductsResult: Result<OrderDelivery> = Result.success(OrderDelivery.SENT)

    /** Like the device does, the lines show up in the order at once, also while they wait to be sent. */
    override suspend fun addProducts(orderId: String, lines: List<Product>, employeeId: String): Result<OrderDelivery> {
        val delivery = addProductsResult.getOrElse { return addProductsResult }
        openOrders.value = openOrders.value.map { order ->
            if (order.id != orderId) order
            else order.copy(products = order.products + lines, isPendingToSend = delivery == OrderDelivery.PENDING)
        }
        return addProductsResult
    }

    override suspend fun removeProduct(orderId: String, lineId: String, employeeId: String): Result<Unit> {
        changeFailure?.let { return Result.failure(it) }
        val order = openOrders.value.firstOrNull { it.id == orderId }
            ?: return Result.failure(OrderActionError.NotFound)
        if (order.state != OrderState.IN_PROGRESS) return Result.failure(OrderActionError.StateChanged(order.state))
        val remaining = order.products.filterNot { it.lineId == lineId }
        if (remaining.isEmpty()) return Result.failure(OrderActionError.LastProduct)
        openOrders.value = openOrders.value.map { if (it.id == orderId) it.copy(products = remaining) else it }
        return Result.success(Unit)
    }

    override suspend fun saveOrder(order: Order): Result<OrderDelivery> {
        attempts += order
        if (result.isSuccess) saved += order
        return result
    }

    override fun observePendingOrderCount(employeeId: String): Flow<Int> =
        pendingOrdersFailure?.let { error -> flow { throw error } }
            ?: pendingOrders.map { it[employeeId] ?: 0 }
}
