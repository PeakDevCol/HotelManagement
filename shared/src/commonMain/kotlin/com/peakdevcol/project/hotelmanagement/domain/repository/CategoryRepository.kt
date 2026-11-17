package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** Emits every category and its changes, in no particular order. */
    fun observeCategories(): Flow<List<Category>>

    /** Creates the categories, or replaces the ones with the same id, all together. */
    suspend fun save(categories: List<Category>): Result<Unit>

    suspend fun delete(categoryId: String): Result<Unit>
}
