package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** RF-INV-01: every category in its order, every product, active or not, and every tax. */
class ObserveInventoryUseCase(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val taxRepository: TaxRepository,
) {
    operator fun invoke(): Flow<Inventory> = combine(
        categoryRepository.observeCategories(),
        productRepository.observeProducts(),
        taxRepository.observeTaxes(),
    ) { categories, products, taxes ->
        Inventory(
            categories = categories.sortedBy { it.position },
            products = products,
            taxes = taxes.sortedBy { it.name.lowercase() },
        )
    }
}
