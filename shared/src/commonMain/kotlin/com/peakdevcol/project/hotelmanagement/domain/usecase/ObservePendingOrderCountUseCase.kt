package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** RF-ORD-09: how many orders of the logged-in employee are saved on the device, waiting to be sent. */
class ObservePendingOrderCountUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Int> = authRepository.session.flatMapLatest { employeeId ->
        if (employeeId == null) flowOf(0) else orderRepository.observePendingOrderCount(employeeId)
    }
}
