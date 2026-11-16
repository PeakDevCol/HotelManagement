package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingOrderCountUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObservePendingOrderCountUseCaseTest {
    private val orders = FakeOrderRepository()

    @Test
    fun followsThePendingOrdersOfTheLoggedInEmployee() = runTest {
        val counts = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObservePendingOrderCountUseCase(FakeAuthRepository(userId = "uid-1"), orders)().toList(counts)
        }

        orders.pendingOrders.value = mapOf("uid-1" to 2, "uid-2" to 5)
        orders.pendingOrders.value = mapOf("uid-2" to 5)

        assertEquals(listOf(0, 2, 0), counts)
    }

    @Test
    fun thereIsNothingPendingWithoutASession() = runTest {
        orders.pendingOrders.value = mapOf("uid-1" to 2)
        val counts = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObservePendingOrderCountUseCase(FakeAuthRepository(userId = null), orders)().toList(counts)
        }

        assertEquals(listOf(0), counts)
    }
}
