package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** RF-INV-01: every category in its order and every product, active or not. */
class ObserveInventoryUseCase(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<Inventory> =
        combine(categoryRepository.observeCategories(), productRepository.observeProducts()) { categories, products ->
            Inventory(categories = categories.sortedBy { it.position }, products = products)
        }
}
