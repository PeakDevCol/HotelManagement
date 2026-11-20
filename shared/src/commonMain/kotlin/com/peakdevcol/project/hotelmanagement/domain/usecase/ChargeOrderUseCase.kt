package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

/**
 * RF-CUR-05: records the payment of an order, for its whole amount, with an optional tip. An order
 * in progress is closed first, in its own transaction, so the rules see the same two changes as
 * always (PRD, section 13, 2026-11-19). If the payment fails after closing, the order stays closed
 * and is charged again.
 */
class ChargeOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(order: Order, method: PaymentMethod, tipAmount: Long = 0): Result<Unit> {
        if (tipAmount < 0) return Result.failure(OrderActionError.InvalidTip)
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        if (order.state == OrderState.IN_PROGRESS) {
            val closed = orderRepository.changeState(order.id, OrderStateChange.Close(employeeId))
            if (closed.isFailure) return closed
        }
        return orderRepository.changeState(order.id, OrderStateChange.Pay(employeeId, method, tipAmount))
    }
}
