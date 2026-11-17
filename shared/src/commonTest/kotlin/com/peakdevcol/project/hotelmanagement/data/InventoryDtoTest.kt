package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.CategoryDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.InventoryProductDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toCategory
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import kotlin.test.Test
import kotlin.test.assertEquals

class InventoryDtoTest {
    @Test
    fun aProductIsReadBackAsItWasStored() {
        val product = InventoryProduct(id = "beer", name = "Cerveza Aguila", unitPrice = 3_500, categoryId = "drinks", taxId = "iva", active = false)

        assertEquals(product, product.toDto().toProduct("beer"))
    }

    @Test
    fun theIdOfAProductIsTheIdOfItsDocumentAndIsNotStoredInside() {
        val dto = InventoryProduct(id = "beer", name = "Cerveza", unitPrice = 3_500, categoryId = "drinks").toDto()

        assertEquals(InventoryProductDto(name = "Cerveza", unitPrice = 3_500, categoryId = "drinks", taxId = null, active = true), dto)
    }

    @Test
    fun aCategoryIsReadBackAsItWasStored() {
        val category = Category(id = "drinks", name = "Bebidas", position = 2)

        assertEquals(CategoryDto(name = "Bebidas", position = 2), category.toDto())
        assertEquals(category, category.toDto().toCategory("drinks"))
    }
}
