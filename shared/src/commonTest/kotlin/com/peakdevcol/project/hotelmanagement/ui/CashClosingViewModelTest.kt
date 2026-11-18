package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCashClosingUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeCashClosingRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakePaymentRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.cashClosing
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.payment
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingEffect
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingIntent
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CashClosingViewModelTest : MainDispatcherTest() {
    private val payments = FakePaymentRepository(payment("order_a", "2026-11-17T20:00:00Z", tipAmount = 500))
    private val closings = FakeCashClosingRepository()
    private val users = FakeUserRepository(employee("admin", "Juan Felipe", type = UserType.ADMIN))

    private fun viewModel() = CashClosingViewModel(
        LoadCashClosingUseCase(closings, payments),
        SaveCashClosingUseCase(FakeAuthRepository(userId = "admin"), users, closings),
        ObserveEmployeesUseCase(users),
    )

    private fun CashClosingViewModel.fill(type: CashClosingType? = CashClosingType.DAILY, counted: String = "4000", note: String = "") {
        type?.let { onIntent(CashClosingIntent.SelectType(it)) }
        onIntent(CashClosingIntent.CountedChange(counted))
        onIntent(CashClosingIntent.NoteChange(note))
    }

    @Test
    fun showsTheExpectedCashSinceTheLastClosing() {
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals(4_000, state.overview!!.draft.expectedCash)
        assertEquals(1, state.overview!!.draft.number)
    }

    @Test
    fun showsTheDifferenceWhileTyping() {
        val viewModel = viewModel()

        viewModel.onIntent(CashClosingIntent.CountedChange("$3.500"))

        assertEquals("3500", viewModel.state.value.countedText)
        assertEquals(-500, viewModel.state.value.difference)
    }

    @Test
    fun asksForConfirmationBeforeSaving() {
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.onIntent(CashClosingIntent.RequestSave)

        assertTrue(viewModel.state.value.isConfirming)
        assertTrue(closings.closings.isEmpty())
    }

    @Test
    fun aMissingNoteIsShownWithoutAskingToConfirm() {
        val viewModel = viewModel()
        viewModel.fill(counted = "3000")

        viewModel.onIntent(CashClosingIntent.RequestSave)

        assertEquals(CashClosingError.MissingNote, viewModel.state.value.inputError)
        assertFalse(viewModel.state.value.isConfirming)
    }

    @Test
    fun aMissingTypeIsShown() {
        val viewModel = viewModel()
        viewModel.fill(type = null)

        viewModel.onIntent(CashClosingIntent.RequestSave)

        assertEquals(CashClosingError.MissingType, viewModel.state.value.inputError)
    }

    @Test
    fun savingClearsTheFormAndStartsTheNextClosing() = runTest {
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onIntent(CashClosingIntent.RequestSave)

        viewModel.onIntent(CashClosingIntent.ConfirmSave)

        assertEquals(CashClosingEffect.Saved, viewModel.effects.first())
        val state = viewModel.state.value
        assertEquals(1, closings.closings.single().number)
        assertEquals(2, state.overview!!.draft.number)
        assertEquals(0, state.overview!!.draft.payments.size)
        assertEquals(listOf(1L), state.overview!!.recentClosings.map { it.number })
        assertEquals("", state.countedText)
        assertNull(state.type)
    }

    @Test
    fun whenAnotherAdministratorClosedFirstItReloads() = runTest {
        val viewModel = viewModel()
        closings.closings = listOf(cashClosing(number = 1, to = "2026-11-17T20:00:00Z", createdAt = "2026-11-17T21:00:00Z"))
        viewModel.fill()
        viewModel.onIntent(CashClosingIntent.RequestSave)

        viewModel.onIntent(CashClosingIntent.ConfirmSave)

        assertEquals(CashClosingEffect.SaveFailed(CashClosingError.AlreadyClosed), viewModel.effects.first())
        assertEquals(2, viewModel.state.value.overview!!.draft.number)
        assertEquals(0, viewModel.state.value.overview!!.draft.expectedCash)
    }

    @Test
    fun withoutConnectionItSaysSo() {
        payments.failure = PaymentReadError.NoConnection

        val state = viewModel().state.value

        assertEquals(CashClosingError.NoConnection, state.loadError)
        assertNull(state.overview)
    }
}
