package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.PendingCollection
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** RF-BAL-04: the orders in progress and closed, which are few, so it listens to them in real time. */
class ObservePendingCollectionUseCase(
    private val orderRepository: OrderRepository,
) {
    operator fun invoke(): Flow<PendingCollection> = orderRepository.observeOpenOrders().map(PendingCollection::of)
}
