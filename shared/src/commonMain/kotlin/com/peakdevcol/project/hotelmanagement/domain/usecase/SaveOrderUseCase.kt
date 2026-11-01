package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

class SaveOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    /** Saves a new confirmed order for the logged-in employee and returns it. */
    suspend operator fun invoke(
        products: List<Product>,
        destination: OrderDestination,
    ): Result<Order> {
        if (products.isEmpty()) return Result.failure(OrderError.EmptyOrder)
        val employeeId = authRepository.currentUserId()
            ?: return Result.failure(OrderError.NoSession)

        val order = Order.create(employeeId, products, destination)
        return orderRepository.saveOrder(order).map { order }
    }
}

sealed class OrderError(message: String) : Exception(message) {
    data object EmptyOrder : OrderError("The order has no products")
    data object NoSession : OrderError("There is no logged-in employee")
}
