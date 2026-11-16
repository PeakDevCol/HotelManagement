package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

/** RF-CUR-06: removes one line of an order in progress. An order is never left without products. */
class RemoveProductFromOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String, lineId: String): Result<Unit> {
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        return orderRepository.removeProduct(orderId, lineId, employeeId)
    }
}
