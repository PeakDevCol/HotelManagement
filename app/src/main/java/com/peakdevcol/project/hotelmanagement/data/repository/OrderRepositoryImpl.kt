package com.peakdevcol.project.hotelmanagement.data.repository

import com.google.firebase.Timestamp
import com.peakdevcol.project.hotelmanagement.data.OrderRemoteDataSource
import com.peakdevcol.project.hotelmanagement.domain.ConfirmOrder
import com.peakdevcol.project.hotelmanagement.domain.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.Product
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeOrder
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_DATE
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_EMPLOYEE_ID
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_ID
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_PRODUCTS
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_STATE
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_TOTAL
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_TYPE
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_TYPE_NUMBER
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PRODUCT_AMOUNT
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PRODUCT_ID
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PRODUCT_NAME
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PRODUCT_QUANTITY
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PRODUCT_TOTAL
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val remote: OrderRemoteDataSource
) : OrderRepository {

    override fun currentEmployeeId(): String? = remote.currentUserId()

    override suspend fun saveOrder(order: ConfirmOrder): Boolean =
        remote.saveOrder(order.id, order.toFirestoreMap())

    private fun ConfirmOrder.toFirestoreMap(): Map<String, Any?> = mapOf(
        ORDER_ID to id,
        ORDER_EMPLOYEE_ID to idEmployee,
        ORDER_PRODUCTS to products.map { it.toFirestoreMap() },
        ORDER_TYPE to typeOrder.typeName(),
        ORDER_TYPE_NUMBER to typeOrder.number(),
        ORDER_DATE to Timestamp(dateOrder),
        ORDER_STATE to state.name.lowercase(),
        ORDER_TOTAL to totalPayAmount
    )

    private fun Product.toFirestoreMap(): Map<String, Any> = mapOf(
        PRODUCT_ID to productId,
        PRODUCT_NAME to productName,
        PRODUCT_QUANTITY to (productQuantity.toLongOrNull() ?: 0L),
        PRODUCT_AMOUNT to (productAmount.toLongOrNull() ?: 0L),
        PRODUCT_TOTAL to totalAmount()
    )

    private fun ProviderTypeOrder.typeName(): String = when (this) {
        is ProviderTypeOrder.Room -> "room"
        is ProviderTypeOrder.Table -> "table"
        ProviderTypeOrder.Other -> "other"
    }

    private fun ProviderTypeOrder.number(): Int? = when (this) {
        is ProviderTypeOrder.Room -> roomNumber
        is ProviderTypeOrder.Table -> tableNumber
        ProviderTypeOrder.Other -> null
    }
}
