package com.peakdevcol.project.hotelmanagement.data.catalog

import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductCatalogRepository

/** Fixed product list until the catalog comes from Firestore (RF-ORD-07, M4). */
class StaticProductCatalog : ProductCatalogRepository {
    override suspend fun getProducts(): Result<List<Product>> = Result.success(PRODUCTS)

    private companion object {
        val PRODUCTS = listOf(
            Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500),
            Product(id = "2", name = "Cerveza Poker", unitPrice = 3_500),
            Product(id = "3", name = "Coca-Cola", unitPrice = 3_000),
            Product(id = "4", name = "Corona", unitPrice = 6_000),
            Product(id = "5", name = "Papas BBQ", unitPrice = 2_000),
            Product(id = "6", name = "Bombombun", unitPrice = 1_000),
            Product(id = "7", name = "Piscina", unitPrice = 7_000),
            Product(id = "8", name = "Granizada", unitPrice = 6_500),
            Product(id = "9", name = "Frappe", unitPrice = 4_500),
        )
    }
}
