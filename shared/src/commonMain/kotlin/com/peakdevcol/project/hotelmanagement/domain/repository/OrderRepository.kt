package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    /**
     * Saves the order on the device and tells whether the server confirmed it right away.
     * It fails only when the order could not be saved at all.
     */
    suspend fun saveOrder(order: Order): Result<OrderDelivery>

    /** Emits how many in-progress orders of the employee are still waiting to be sent. */
    fun observePendingOrderCount(employeeId: String): Flow<Int>

    /** Emits the orders in progress and closed, and every change to them, in no particular order. */
    fun observeOpenOrders(): Flow<List<Order>>

    /** Emits the order and its changes, or null when it does not exist. */
    fun observeOrder(orderId: String): Flow<Order?>

    /** Emits the changes recorded after the creation of the order, the oldest first. */
    fun observeOrderEvents(orderId: String): Flow<List<OrderEvent>>

    /**
     * Moves the order to the state of the change, only if the order is still in a state that
     * allows it, and records the event. A payment is recorded together with it, never twice.
     * It fails with an [OrderActionError][com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError].
     */
    suspend fun changeState(orderId: String, change: OrderStateChange): Result<Unit>

    /**
     * RF-CUR-13: adds the lines to the order on the device and tells whether the server confirmed
     * them right away. Lines added from several devices are all kept. If the order is no longer in
     * progress when they reach the server, the server rejects them without telling this call.
     */
    suspend fun addProducts(orderId: String, lines: List<Product>, employeeId: String): Result<OrderDelivery>

    /** RF-CUR-06: removes one line of an order in progress. It needs the server and never leaves the order empty. */
    suspend fun removeProduct(orderId: String, lineId: String, employeeId: String): Result<Unit>
}
