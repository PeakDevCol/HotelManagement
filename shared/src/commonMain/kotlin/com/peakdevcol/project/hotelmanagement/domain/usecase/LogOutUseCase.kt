package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

enum class LogOutOutcome {
    LOGGED_OUT,
    BLOCKED_BY_PENDING_ORDERS,
}

/**
 * RF-AUTH-03: the session cannot be closed while orders wait to be sent, because the server
 * would reject them without the session of the employee who took them (R5).
 */
class LogOutUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val pendingAdditionRepository: PendingAdditionRepository,
) {
    suspend operator fun invoke(): LogOutOutcome {
        if (pendingOrderCount() > 0 || hasProductsWaitingToBeSent()) return LogOutOutcome.BLOCKED_BY_PENDING_ORDERS
        authRepository.logOut()
        return LogOutOutcome.LOGGED_OUT
    }

    // An account that cannot read its orders, like a deactivated one, has nothing left to send,
    // and an answer that never comes must not keep the employee from logging out (RNF-04).
    private suspend fun pendingOrderCount(): Int {
        val employeeId = authRepository.currentUserId() ?: return 0
        return try {
            withTimeoutOrNull(PENDING_ORDERS_WAIT) { orderRepository.observePendingOrderCount(employeeId).first() } ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun hasProductsWaitingToBeSent(): Boolean =
        pendingAdditionRepository.observePendingAdditions().first().any { !it.rejected }

    private companion object {
        val PENDING_ORDERS_WAIT = 3.seconds
    }
}
