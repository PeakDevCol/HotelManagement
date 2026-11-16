package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.isAllowedFor
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** RF-CUR-07: an administrator cancels an order with a reason. The order is kept, never deleted. */
class CancelOrderUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(orderId: String, reason: String): Result<Unit> {
        val cleanReason = reason.trim()
        if (cleanReason.isEmpty()) return Result.failure(OrderActionError.MissingReason)
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        val employee = userRepository.observeUser(employeeId).first()
        if (employee == null || !OrderAction.CANCEL.isAllowedFor(employee.type)) {
            return Result.failure(OrderActionError.NotAllowed)
        }
        return orderRepository.changeState(orderId, OrderStateChange.Cancel(employeeId, cleanReason))
    }
}
