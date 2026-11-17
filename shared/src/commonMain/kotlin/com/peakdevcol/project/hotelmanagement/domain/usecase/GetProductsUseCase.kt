package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * RF-ORD-07: the products that can be ordered, grouped by category in the order the administrator
 * gave them. Inactive products and empty categories are left out.
 */
class GetProductsUseCase(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(): Result<List<CatalogGroup>> = try {
        val inventory = Inventory(
            categories = categoryRepository.observeCategories().first().sortedBy { it.position },
            products = productRepository.observeProducts().first().filter { it.active },
        )
        val groups = inventory.categories.map { CatalogGroup(it.name, inventory.productsOf(it).toOrderProducts()) } +
            CatalogGroup(categoryName = null, products = inventory.productsWithoutCategory.toOrderProducts())
        Result.success(groups.filter { it.products.isNotEmpty() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun List<InventoryProduct>.toOrderProducts(): List<Product> =
        map { Product(id = it.id, name = it.name, unitPrice = it.unitPrice) }
}
