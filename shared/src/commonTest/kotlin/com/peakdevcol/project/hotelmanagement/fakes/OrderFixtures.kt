package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import kotlin.time.Instant

fun employee(id: String, fullName: String, type: UserType = UserType.BASIC) = User(
    id = id,
    fullName = fullName,
    email = "$id@hotel.co",
    type = type,
    area = UserArea.SERVICE,
    active = true,
)

fun order(
    id: String,
    employeeId: String = "uid-1",
    state: OrderState = OrderState.IN_PROGRESS,
    createdAt: String? = "2026-11-15T00:23:00Z",
    destination: OrderDestination = OrderDestination.Table(3),
    isPendingToSend: Boolean = false,
    isUpToDate: Boolean = true,
    products: List<Product> = listOf(Product(id = "1", name = "Cerveza Aguila", unitPrice = 3_500, quantity = 2, lineId = "line-0")),
) = Order(
    id = id,
    employeeId = employeeId,
    products = products,
    destination = destination,
    state = state,
    createdAt = createdAt?.let(Instant::parse),
    isPendingToSend = isPendingToSend,
    isUpToDate = isUpToDate,
)
