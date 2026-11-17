package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * RF-CUR-13 (V6): finds out what the server did with the products added without connection.
 * Firestore drops a rejected write without telling, so each pending addition is compared with
 * the order the server has: the lines that are missing were rejected.
 * It runs for as long as it is collected.
 */
class ReconcilePendingAdditionsUseCase(
    private val orderRepository: OrderRepository,
    private val pendingAdditionRepository: PendingAdditionRepository,
) {
    suspend operator fun invoke() {
        unresolvedAdditions().collect()
    }

    // Resolving an addition changes the list, which starts the wait again with what is left.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun unresolvedAdditions(): Flow<Unit> =
        pendingAdditionRepository.observePendingAdditions()
            .map { additions -> additions.filterNot { it.rejected } }
            .flatMapLatest { additions ->
                val next = additions.firstOrNull() ?: return@flatMapLatest emptyFlow()
                flow { emit(resolve(next, orderAnsweredByTheServer(next.orderId))) }
            }

    // The device's own copy still shows the lines it is trying to send, so only an order that
    // comes from the server and has nothing left to send says what happened to them.
    private suspend fun orderAnsweredByTheServer(orderId: String): Order =
        orderRepository.observeOrder(orderId)
            .filterNotNull()
            .first { order -> order.isUpToDate && !order.isPendingToSend }

    private suspend fun resolve(addition: PendingAddition, order: Order) {
        val acceptedLineIds = order.products.mapNotNull { it.lineId }.toSet()
        val rejectedProducts = addition.products.filterNot { it.lineId in acceptedLineIds }
        if (rejectedProducts.isEmpty()) {
            pendingAdditionRepository.remove(setOf(addition.id))
        } else {
            pendingAdditionRepository.save(addition.copy(products = rejectedProducts, rejected = true))
        }
    }
}
