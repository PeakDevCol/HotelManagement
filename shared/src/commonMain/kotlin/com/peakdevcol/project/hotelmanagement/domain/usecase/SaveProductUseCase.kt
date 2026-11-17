package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository

/**
 * RF-INV-02 and RF-INV-03: creates or edits a product of the inventory, also to deactivate it.
 * Orders keep the name and the price they copied, so editing a product never changes them.
 */
class SaveProductUseCase(
    private val productRepository: ProductRepository,
) {
    /** A null [productId] creates the product. */
    suspend operator fun invoke(
        productId: String?,
        name: String,
        unitPrice: Long?,
        categoryId: String?,
        taxId: String? = null,
        active: Boolean = true,
    ): Result<Unit> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Result.failure(InventoryError.BlankName)
        if (unitPrice == null || unitPrice <= 0) return Result.failure(InventoryError.InvalidPrice)
        if (categoryId.isNullOrEmpty()) return Result.failure(InventoryError.MissingCategory)
        val product = InventoryProduct(
            id = productId ?: newId(),
            name = cleanName,
            unitPrice = unitPrice,
            categoryId = categoryId,
            taxId = taxId,
            active = active,
        )
        return productRepository.save(product)
    }
}
