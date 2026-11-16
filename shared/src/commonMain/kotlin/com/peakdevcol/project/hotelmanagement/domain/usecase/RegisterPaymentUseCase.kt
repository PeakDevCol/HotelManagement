package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

/** RF-CUR-05: records the payment of a closed order, for its whole amount, with an optional tip. */
class RegisterPaymentUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String, method: PaymentMethod, tipAmount: Long = 0): Result<Unit> {
        if (tipAmount < 0) return Result.failure(OrderActionError.InvalidTip)
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        return orderRepository.changeState(orderId, OrderStateChange.Pay(employeeId, method, tipAmount))
    }
}
