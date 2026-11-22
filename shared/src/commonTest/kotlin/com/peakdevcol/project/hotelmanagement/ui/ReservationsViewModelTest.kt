package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveUpcomingReservationsUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeReservationRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.clockAt
import com.peakdevcol.project.hotelmanagement.fakes.day
import com.peakdevcol.project.hotelmanagement.fakes.reservation
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationsEffect
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationsIntent
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Decision 2A of M11: "Próximas". */
class ReservationsViewModelTest : MainDispatcherTest() {
    private val reservations = FakeReservationRepository(
        reservation("ana", roomId = "r-204", checkIn = "2026-11-22", checkOut = "2026-11-24", roomNumber = 204, guestName = "Ana María Rojas"),
        reservation("luis", roomId = "r-101", checkIn = "2026-11-25", checkOut = "2026-11-26", roomNumber = 101, guestName = "Luis Pérez"),
        reservation("marta", roomId = "r-305", checkIn = "2026-11-22", checkOut = "2026-11-23", roomNumber = 305, guestName = "Marta Gómez"),
    )

    private fun viewModel() = ReservationsViewModel(ObserveUpcomingReservationsUseCase(reservations, FakeSettingsRepository(), clockAt("2026-11-22T15:00:00Z")))

    @Test
    fun theReservationsAreGroupedByArrivalDayAndRoomLive() {
        val viewModel = viewModel()

        val days = viewModel.state.value.arrivalDays
        assertEquals(listOf(day("2026-11-22"), day("2026-11-25")), days.map { it.date })
        assertEquals(listOf("ana", "marta"), days.first().reservations.map { it.id })

        reservations.reservations.value += reservation("new", roomId = "r-102", checkIn = "2026-11-25", checkOut = "2026-11-27", roomNumber = 102)

        assertEquals(listOf("luis", "new"), viewModel.state.value.arrivalDays.last().reservations.map { it.id })
    }

    @Test
    fun theListNarrowsByGuestOrRoom() {
        val viewModel = viewModel()

        viewModel.onIntent(ReservationsIntent.QueryChange("roj"))
        assertEquals(listOf("ana"), viewModel.state.value.arrivalDays.flatMap { it.reservations }.map { it.id })

        viewModel.onIntent(ReservationsIntent.QueryChange("30"))
        assertEquals(listOf("marta"), viewModel.state.value.arrivalDays.flatMap { it.reservations }.map { it.id })

        viewModel.onIntent(ReservationsIntent.QueryChange("nadie"))
        assertTrue(viewModel.state.value.arrivalDays.isEmpty())
    }

    @Test
    fun aReservationOpensAndCloses() {
        val viewModel = viewModel()

        viewModel.onIntent(ReservationsIntent.OpenReservation("luis"))
        assertEquals("Luis Pérez", viewModel.state.value.openReservation?.guestName)

        viewModel.onIntent(ReservationsIntent.CloseReservation)
        assertNull(viewModel.state.value.openReservation)
    }

    @Test
    fun aNewReservationOpensItsScreen() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(ReservationsIntent.NewReservation)

        assertEquals(ReservationsEffect.NavigateToNewReservation, viewModel.effects.first())
    }

    @Test
    fun aListThatCouldNotBeReadIsRetried() {
        reservations.readFailure = ReservationError.NoConnection
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.loadFailed)

        reservations.readFailure = null
        viewModel.onIntent(ReservationsIntent.Retry)

        assertEquals(3, viewModel.state.value.reservations.size)
    }
}
