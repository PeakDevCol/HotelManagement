package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    /** Emits every product of the inventory, active or not, and its changes, in no particular order. */
    fun observeProducts(): Flow<List<InventoryProduct>>

    /** Creates the product, or replaces the one with the same id. Products are deactivated, never deleted. */
    suspend fun save(product: InventoryProduct): Result<Unit>
}
