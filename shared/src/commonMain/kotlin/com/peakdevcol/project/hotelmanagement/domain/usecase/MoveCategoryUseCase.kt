package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

enum class MoveDirection { UP, DOWN }

/** RF-INV-04: changes the order of the categories, which is the order of the product picker. */
class MoveCategoryUseCase(
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: String, direction: MoveDirection): Result<Unit> {
        val ordered = categoryRepository.observeCategories().first().sortedBy { it.position }
        val from = ordered.indexOfFirst { it.id == categoryId }
        val to = if (direction == MoveDirection.UP) from - 1 else from + 1
        if (from < 0 || to !in ordered.indices) return Result.success(Unit)

        val moved = ordered.toMutableList().apply { add(to, removeAt(from)) }
        // Every position is written again, so gaps or repeated positions left by old data get fixed.
        return categoryRepository.save(moved.mapIndexed { index, category -> category.copy(position = index) })
    }
}
