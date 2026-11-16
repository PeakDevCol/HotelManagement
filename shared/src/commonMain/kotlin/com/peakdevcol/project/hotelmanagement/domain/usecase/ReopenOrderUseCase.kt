package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

/** RF-CUR-10: puts a closed order back in progress, so it can be edited again. */
class ReopenOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String): Result<Unit> {
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        return orderRepository.changeState(orderId, OrderStateChange.Reopen(employeeId))
    }
}
