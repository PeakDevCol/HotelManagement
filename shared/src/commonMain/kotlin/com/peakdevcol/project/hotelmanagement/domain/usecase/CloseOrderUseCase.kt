package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

/** RF-CUR-04: closes the bill of an order in progress, which leaves it waiting for the payment. */
class CloseOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String): Result<Unit> {
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        return orderRepository.changeState(orderId, OrderStateChange.Close(employeeId))
    }
}
