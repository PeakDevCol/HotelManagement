package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductCatalogRepository

class GetProductsUseCase(private val catalog: ProductCatalogRepository) {
    /** Products sorted by name, ready to show in a picker. */
    suspend operator fun invoke(): Result<List<Product>> =
        catalog.getProducts().map { products -> products.sortedBy { it.name.lowercase() } }
}
