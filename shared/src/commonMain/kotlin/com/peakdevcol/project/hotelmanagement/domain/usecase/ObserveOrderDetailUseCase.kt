package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** RF-CUR-03: the detail of an order and its history, or null when the order does not exist. */
class ObserveOrderDetailUseCase(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(orderId: String): Flow<OrderDetail?> =
        combine(orderRepository.observeOrder(orderId), userRepository.observeEmployees()) { order, employees ->
            order?.let {
                val employeeName = employees.firstOrNull { it.id == order.employeeId }?.fullName
                OrderDetail(order = order, employeeName = employeeName, history = listOf(order.creationEvent(employeeName)))
            }
        }

    // RF-CUR-11: the order itself records who created it and when, so that event needs no document of its own.
    private fun Order.creationEvent(employeeName: String?) =
        OrderEvent(type = OrderEventType.CREATED, employeeName = employeeName, at = createdAt)
}
