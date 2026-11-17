package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first

/** RF-INV-04: deletes a category that has no products. Products are never deleted, so theirs must be moved first. */
class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
) {
    suspend operator fun invoke(categoryId: String): Result<Unit> {
        val inUse = productRepository.observeProducts().first().any { it.categoryId == categoryId }
        if (inUse) return Result.failure(InventoryError.CategoryInUse)
        return categoryRepository.delete(categoryId)
    }
}
