package com.peakdevcol.project.hotelmanagement.domain

import javax.inject.Inject

class SaveOrderUseCase @Inject constructor(private val orderRepository: OrderRepository) {

    suspend operator fun invoke(products: List<Product>, typeOrder: ProviderTypeOrder): Boolean {
        if (products.isEmpty()) return false
        val employeeId = orderRepository.currentEmployeeId() ?: return false
        return orderRepository.saveOrder(
            ConfirmOrder.create(
                idEmployee = employeeId,
                products = products,
                typeOrder = typeOrder
            )
        )
    }
}
