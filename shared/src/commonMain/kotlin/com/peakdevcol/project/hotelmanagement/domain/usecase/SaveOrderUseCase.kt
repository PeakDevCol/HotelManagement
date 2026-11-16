package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository

class SaveOrderUseCase(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) {
    /**
     * Saves a new order in progress for the logged-in employee and tells whether the server
     * already has it. Pass the same [orderId] when retrying, so the order is not duplicated.
     */
    suspend operator fun invoke(
        products: List<Product>,
        destination: OrderDestination,
        orderId: String = Order.newId(),
    ): Result<OrderDelivery> {
        if (products.isEmpty()) return Result.failure(OrderError.EmptyOrder)
        val employeeId = authRepository.currentUserId()
            ?: return Result.failure(OrderError.NoSession)

        val order = Order.create(employeeId, products, destination, id = orderId)
        return orderRepository.saveOrder(order)
    }
}

sealed class OrderError(message: String) : Exception(message) {
    data object EmptyOrder : OrderError("The order has no products")
    data object NoSession : OrderError("There is no logged-in employee")
}
