package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository

/** RF-CUR-06 and RF-CUR-13: adds products to an order in progress, also without connection. */
class AddProductsToOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val pendingAdditionRepository: PendingAdditionRepository,
) {
    suspend operator fun invoke(order: Order, products: List<Product>): Result<OrderDelivery> {
        if (products.isEmpty()) return Result.failure(OrderActionError.NoProducts)
        val employeeId = authRepository.currentUserId() ?: return Result.failure(OrderActionError.NoSession)
        val addition = PendingAddition(
            id = Order.newId(),
            orderId = order.id,
            destination = order.destination,
            products = Order.linesOf(products),
        )

        // Recorded before writing, so an app closed in the middle still knows what it was sending.
        pendingAdditionRepository.save(addition)
        val result = orderRepository.addProducts(order.id, addition.products, employeeId)
        if (result.getOrNull() != OrderDelivery.PENDING) pendingAdditionRepository.remove(setOf(addition.id))
        return result
    }
}
