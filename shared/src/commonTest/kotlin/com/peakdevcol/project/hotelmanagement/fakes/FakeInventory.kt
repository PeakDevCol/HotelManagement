package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.CategoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetProductsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeProductRepository(vararg products: InventoryProduct) : ProductRepository {
    val products = MutableStateFlow(products.toList())
    var readFailure: Throwable? = null
    var saveFailure: InventoryError? = null

    override fun observeProducts(): Flow<List<InventoryProduct>> =
        readFailure?.let { error -> flow { throw error } } ?: products

    override suspend fun save(product: InventoryProduct): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        products.value = products.value.filterNot { it.id == product.id } + product
        return Result.success(Unit)
    }
}

class FakeCategoryRepository(vararg categories: Category) : CategoryRepository {
    val categories = MutableStateFlow(categories.toList())
    var saveFailure: InventoryError? = null

    override fun observeCategories(): Flow<List<Category>> = categories

    override suspend fun save(categories: List<Category>): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        val savedIds = categories.map { it.id }.toSet()
        this.categories.value = this.categories.value.filterNot { it.id in savedIds } + categories
        return Result.success(Unit)
    }

    override suspend fun delete(categoryId: String): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        categories.value = categories.value.filterNot { it.id == categoryId }
        return Result.success(Unit)
    }
}

/** An inventory with a single category, for the screens that only need something to order. */
class FakeCatalog(vararg products: Product) {
    val categoryName = "Bebidas"
    val productRepository = FakeProductRepository()
    private val categoryRepository = FakeCategoryRepository(Category(id = "drinks", name = categoryName, position = 0))
    val getProducts = GetProductsUseCase(productRepository, categoryRepository)

    init {
        offer(products.toList())
    }

    fun offer(products: List<Product>) {
        productRepository.products.value =
            products.map { InventoryProduct(id = it.id, name = it.name, unitPrice = it.unitPrice, categoryId = "drinks") }
    }

    fun groupOf(vararg products: Product) = listOf(CatalogGroup(categoryName, products.toList()))
}
