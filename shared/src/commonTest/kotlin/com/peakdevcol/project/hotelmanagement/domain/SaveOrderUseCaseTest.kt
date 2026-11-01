package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.OrderError
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOrderUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveOrderUseCaseTest {

    private val product = Product(id = "3", name = "Coca-Cola", unitPrice = 3_000, quantity = 3)

    @Test
    fun savesOrderForLoggedInEmployee() = runTest {
        val orders = FakeOrderRepository()
        val useCase = SaveOrderUseCase(FakeAuthRepository(userId = "uid-1"), orders)

        val result = useCase(listOf(product), OrderDestination.Table(4))

        assertTrue(result.isSuccess)
        val saved = orders.saved.single()
        assertEquals("uid-1", saved.employeeId)
        assertEquals(OrderDestination.Table(4), saved.destination)
        assertEquals(9_000, saved.totalAmount)
    }

    @Test
    fun failsWhenOrderHasNoProducts() = runTest {
        val orders = FakeOrderRepository()
        val useCase = SaveOrderUseCase(FakeAuthRepository(userId = "uid-1"), orders)

        val result = useCase(emptyList(), OrderDestination.Other)

        assertEquals(OrderError.EmptyOrder, result.exceptionOrNull())
        assertTrue(orders.saved.isEmpty())
    }

    @Test
    fun failsWhenThereIsNoSession() = runTest {
        val orders = FakeOrderRepository()
        val useCase = SaveOrderUseCase(FakeAuthRepository(userId = null), orders)

        val result = useCase(listOf(product), OrderDestination.Other)

        assertEquals(OrderError.NoSession, result.exceptionOrNull())
        assertTrue(orders.saved.isEmpty())
    }

    @Test
    fun propagatesRepositoryErrors() = runTest {
        val error = IllegalStateException("offline")
        val useCase = SaveOrderUseCase(
            FakeAuthRepository(userId = "uid-1"),
            FakeOrderRepository(result = Result.failure(error)),
        )

        val result = useCase(listOf(product), OrderDestination.Other)

        assertEquals(error, result.exceptionOrNull())
    }
}

private class FakeOrderRepository(
    private val result: Result<Unit> = Result.success(Unit),
) : OrderRepository {
    val saved = mutableListOf<Order>()
    override suspend fun saveOrder(order: Order): Result<Unit> {
        if (result.isSuccess) saved += order
        return result
    }
}
