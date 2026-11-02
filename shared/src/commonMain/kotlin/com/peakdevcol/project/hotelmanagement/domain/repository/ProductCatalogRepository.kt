package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Product

/** Products an employee can add to an order. */
interface ProductCatalogRepository {
    /** Every product with quantity 1. */
    suspend fun getProducts(): Result<List<Product>>
}
