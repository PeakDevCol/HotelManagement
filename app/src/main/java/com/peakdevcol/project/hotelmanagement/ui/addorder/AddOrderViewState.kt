package com.peakdevcol.project.hotelmanagement.ui.addorder

import com.peakdevcol.project.hotelmanagement.domain.Product
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeOrder
import com.peakdevcol.project.hotelmanagement.domain.totalAmount

data class AddOrderViewState(
    val products: List<Product> = emptyList(),
    val typeOrder: ProviderTypeOrder? = null,
    val isLoading: Boolean = false,
) {
    val totalAmount: Long get() = products.totalAmount()

    val confirmButtonEnable: Boolean
        get() = products.isNotEmpty() && typeOrder != null && !isLoading
}
