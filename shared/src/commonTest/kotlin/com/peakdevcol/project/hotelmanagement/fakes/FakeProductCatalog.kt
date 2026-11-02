package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.ProductCatalogRepository

class FakeProductCatalog(
    var result: Result<List<Product>> = Result.success(emptyList()),
) : ProductCatalogRepository {
    override suspend fun getProducts(): Result<List<Product>> = result
}
