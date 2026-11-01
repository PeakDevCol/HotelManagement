package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Order

interface OrderRepository {
    suspend fun saveOrder(order: Order): Result<Unit>
}
