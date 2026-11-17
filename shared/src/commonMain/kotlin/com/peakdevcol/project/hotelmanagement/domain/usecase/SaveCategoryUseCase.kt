package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

/** RF-INV-04: creates a category, at the end of the list, or renames one. Names are not repeated. */
class SaveCategoryUseCase(
    private val categoryRepository: CategoryRepository,
) {
    /** A null [categoryId] creates the category. */
    suspend operator fun invoke(categoryId: String?, name: String): Result<Unit> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Result.failure(InventoryError.BlankName)
        val categories = categoryRepository.observeCategories().first()
        if (categories.any { it.id != categoryId && it.name.equals(cleanName, ignoreCase = true) }) {
            return Result.failure(InventoryError.DuplicateCategory)
        }
        val category = if (categoryId == null) {
            Category(id = newId(), name = cleanName, position = (categories.maxOfOrNull { it.position } ?: -1) + 1)
        } else {
            categories.firstOrNull { it.id == categoryId }?.copy(name = cleanName)
                ?: return Result.failure(InventoryError.NotFound)
        }
        return categoryRepository.save(listOf(category))
    }
}
