package com.peakdevcol.project.hotelmanagement.domain

interface OrderRepository {
    fun currentEmployeeId(): String?
    suspend fun saveOrder(order: ConfirmOrder): Boolean
}
