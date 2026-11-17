package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import kotlinx.serialization.Serializable

/** A category stored at `Categories/{id}`. */
@Serializable
internal data class CategoryDto(
    val name: String = "",
    val position: Int = 0,
)

internal fun Category.toDto() = CategoryDto(name = name, position = position)

internal fun CategoryDto.toCategory(id: String) = Category(id = id, name = name, position = position)

/** A tax stored at `Taxes/{id}`. The rate is in hundredths of a point: 19 % is 1900. */
@Serializable
internal data class TaxDto(
    val name: String = "",
    val rate: Int = 0,
    val active: Boolean = true,
)

internal fun Tax.toDto() = TaxDto(name = name, rate = rate, active = active)

internal fun TaxDto.toTax(id: String) = Tax(id = id, name = name, rate = rate, active = active)

/** A product stored at `Products/{id}`. The lines of an order use [ProductDto], which copies name and price. */
@Serializable
internal data class InventoryProductDto(
    val name: String = "",
    val unitPrice: Long = 0,
    val categoryId: String = "",
    val taxId: String? = null,
    val active: Boolean = true,
)

internal fun InventoryProduct.toDto() =
    InventoryProductDto(name = name, unitPrice = unitPrice, categoryId = categoryId, taxId = taxId, active = active)

internal fun InventoryProductDto.toProduct(id: String) =
    InventoryProduct(id = id, name = name, unitPrice = unitPrice, categoryId = categoryId, taxId = taxId, active = active)
