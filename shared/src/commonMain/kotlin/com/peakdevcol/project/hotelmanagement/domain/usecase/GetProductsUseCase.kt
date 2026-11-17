package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * RF-ORD-07: the products that can be ordered, grouped by category in the order the administrator
 * gave them. Inactive products and empty categories are left out. Each product carries the rate
 * of its tax (RF-INV-07).
 */
class GetProductsUseCase(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val taxRepository: TaxRepository,
) {
    suspend operator fun invoke(): Result<List<CatalogGroup>> = try {
        val inventory = Inventory(
            categories = categoryRepository.observeCategories().first().sortedBy { it.position },
            products = productRepository.observeProducts().first().filter { it.active },
            taxes = taxRepository.observeTaxes().first(),
        )
        val groups = inventory.categories.map { CatalogGroup(it.name, inventory.orderProducts(inventory.productsOf(it))) } +
            CatalogGroup(categoryName = null, products = inventory.orderProducts(inventory.productsWithoutCategory))
        Result.success(groups.filter { it.products.isNotEmpty() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // The order copies the rate with the name and the price, so later changes to the tax do not alter it.
    private fun Inventory.orderProducts(products: List<InventoryProduct>): List<Product> =
        products.map { Product(id = it.id, name = it.name, unitPrice = it.unitPrice, taxRate = appliedTaxRateOf(it)) }
}
