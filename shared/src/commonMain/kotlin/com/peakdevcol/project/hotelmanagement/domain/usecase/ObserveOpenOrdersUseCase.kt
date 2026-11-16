package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Instant

/** RF-CUR-01: the orders in progress and closed, the oldest first, with who took each one. */
class ObserveOpenOrdersUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Flow<List<OrderListing>> =
        combine(orderRepository.observeOpenOrders(), userRepository.observeEmployees()) { orders, employees ->
            val namesById = employees.associate { it.id to it.fullName }
            val currentEmployeeId = authRepository.currentUserId()
            orders
                .sortedBy { it.createdAt ?: Instant.DISTANT_FUTURE }
                .map { order ->
                    OrderListing(
                        order = order,
                        employeeName = namesById[order.employeeId],
                        isMine = order.employeeId == currentEmployeeId,
                    )
                }
        }
}
