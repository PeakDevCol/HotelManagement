package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOrderDetailUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.order
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailIntent
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailState
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OrderDetailViewModelTest : MainDispatcherTest() {
    private val orders = FakeOrderRepository().apply { openOrders.value = listOf(order("a")) }
    private val users = FakeUserRepository(employee("uid-1", "Ana Gómez"))

    private fun viewModel(orderId: String = "a") =
        OrderDetailViewModel(orderId, ObserveOrderDetailUseCase(orders, users))

    @Test
    fun showsTheOrder() {
        val state = assertIs<OrderDetailState.Loaded>(viewModel().state.value)

        assertEquals("a", state.detail.order.id)
        assertEquals("Ana Gómez", state.detail.employeeName)
    }

    @Test
    fun followsTheChangesOfTheOrder() {
        val viewModel = viewModel()

        orders.openOrders.value = listOf(order("a", state = OrderState.CLOSED))

        val state = assertIs<OrderDetailState.Loaded>(viewModel.state.value)
        assertEquals(OrderState.CLOSED, state.detail.order.state)
    }

    @Test
    fun tellsWhenTheOrderDoesNotExist() {
        assertEquals(OrderDetailState.NotFound, viewModel(orderId = "missing").state.value)
    }

    @Test
    fun showsAnErrorAndRetries() {
        orders.openOrdersFailure = IllegalStateException("permission denied")
        val viewModel = viewModel()
        assertEquals(OrderDetailState.LoadFailed, viewModel.state.value)

        orders.openOrdersFailure = null
        viewModel.onIntent(OrderDetailIntent.Retry)

        assertIs<OrderDetailState.Loaded>(viewModel.state.value)
    }
}
