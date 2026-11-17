package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.InventoryProductDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toProduct
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

internal class FirestoreProductRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
) : ProductRepository {

    private val products get() = firestore.value.collection(FirestorePaths.PRODUCTS)

    // RNF-12: the whole catalog is one small collection, read once and then kept up to date from the cache.
    override fun observeProducts(): Flow<List<InventoryProduct>> =
        products.snapshots.map { snapshot -> snapshot.documents.map { it.data<InventoryProductDto>().toProduct(it.id) } }

    override suspend fun save(product: InventoryProduct): Result<Unit> = inventoryWrite(serverWait) {
        products.document(product.id).set(product.toDto())
    }
}
