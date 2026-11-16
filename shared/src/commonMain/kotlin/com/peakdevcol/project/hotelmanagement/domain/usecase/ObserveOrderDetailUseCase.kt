package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.actionsFor
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** RF-CUR-03: the detail of an order and its history, or null when the order does not exist. */
class ObserveOrderDetailUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(orderId: String): Flow<OrderDetail?> = combine(
        orderRepository.observeOrder(orderId),
        orderRepository.observeOrderEvents(orderId),
        userRepository.observeEmployees(),
    ) { order, events, employees ->
        order?.let { detailOf(order, events, employees) }
    }

    private fun detailOf(order: Order, events: List<OrderEvent>, employees: List<User>): OrderDetail {
        val namesById = employees.associate { it.id to it.fullName }
        val currentEmployee = employees.firstOrNull { it.id == authRepository.currentUserId() }
        val history = (listOf(order.creationEvent()) + events).map { it.copy(employeeName = namesById[it.employeeId]) }
        return OrderDetail(
            order = order,
            employeeName = namesById[order.employeeId],
            history = history,
            availableActions = currentEmployee?.let { order.state.actionsFor(it.type) }.orEmpty(),
        )
    }

    // RF-CUR-11: the order itself records who created it and when, so that event needs no document of its own.
    private fun Order.creationEvent() =
        OrderEvent(type = OrderEventType.CREATED, employeeId = employeeId, at = createdAt)
}
