package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.CategoryDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toCategory
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class FirestoreCategoryRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
) : CategoryRepository {

    private val categories get() = firestore.value.collection(FirestorePaths.CATEGORIES)

    override fun observeCategories(): Flow<List<Category>> =
        categories.snapshots.map { snapshot -> snapshot.documents.map { it.data<CategoryDto>().toCategory(it.id) } }

    override suspend fun save(categories: List<Category>): Result<Unit> = inventoryWrite(serverWait) {
        val batch = firestore.value.batch()
        categories.forEach { category -> batch.set(this.categories.document(category.id), category.toDto()) }
        batch.commit()
    }

    override suspend fun delete(categoryId: String): Result<Unit> = inventoryWrite(serverWait) {
        categories.document(categoryId).delete()
    }
}
