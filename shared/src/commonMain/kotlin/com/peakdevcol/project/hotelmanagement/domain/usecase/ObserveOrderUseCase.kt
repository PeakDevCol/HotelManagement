package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow

/** RF-CUR-06: one order as it changes, without its history. Null when it does not exist. */
class ObserveOrderUseCase(private val orderRepository: OrderRepository) {
    operator fun invoke(orderId: String): Flow<Order?> = orderRepository.observeOrder(orderId)
}
