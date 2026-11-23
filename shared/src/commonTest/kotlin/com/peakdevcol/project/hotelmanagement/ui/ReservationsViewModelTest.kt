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
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadReservationHistoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkNoShowUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveTodayReservationsUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.room
import com.peakdevcol.project.hotelmanagement.ui.reservations.HistoryLoad
import com.peakdevcol.project.hotelmanagement.ui.reservations.ReservationsTab
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

    private val clock = clockAt("2026-11-22T15:00:00Z")
    private val settings = FakeSettingsRepository()
    private val rooms = FakeRoomRepository(room("r-204", 204), room("r-305", 305).copy(housekeeping = Housekeeping.DIRTY), room("r-101", 101))
    private val users = FakeUserRepository(employee("reception", "Carlos Gómez", UserType.RECEPTION))
    private val auth = FakeAuthRepository("reception")

    private fun viewModel() = ReservationsViewModel(
        observeUpcomingReservations = ObserveUpcomingReservationsUseCase(reservations, settings, clock),
        observeTodayReservations = ObserveTodayReservationsUseCase(reservations, rooms, settings, clock),
        loadReservationHistory = LoadReservationHistoryUseCase(reservations, users),
        cancelReservation = CancelReservationUseCase(auth, users, reservations),
        markNoShow = MarkNoShowUseCase(auth, users, reservations, settings, clock),
    )

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
    fun itOpensOnTodayWithTheArrivalsAndTheirRoomAndTheReservationsThatDidNotArrive() {
        reservations.reservations.value += reservation("late", roomId = "r-101", checkIn = "2026-11-20", checkOut = "2026-11-23", roomNumber = 101, guestName = "Pedro Ruiz")
        val viewModel = viewModel()

        val state = viewModel.state.value
        assertEquals(ReservationsTab.TODAY, state.tab)
        assertEquals(listOf("ana", "marta"), state.today?.arrivals?.map { it.reservation.id })
        assertEquals(RoomStatus.DIRTY, state.today?.arrivals?.last()?.roomStatus)
        assertEquals(listOf("late"), state.today?.overdue?.map { it.id })

        viewModel.onIntent(ReservationsIntent.SelectTab(ReservationsTab.UPCOMING))
        assertEquals(ReservationsTab.UPCOMING, viewModel.state.value.tab)
    }

    @Test
    fun theDetailReadsItsHistoryAndOnlyAnOverdueReservationIsMarkedAsNotArrived() {
        reservations.reservations.value += reservation("late", roomId = "r-101", checkIn = "2026-11-20", checkOut = "2026-11-23", roomNumber = 101, guestName = "Pedro Ruiz")
        val viewModel = viewModel()

        viewModel.onIntent(ReservationsIntent.OpenReservation("ana"))
        assertIs<HistoryLoad.Loaded>(viewModel.state.value.history)
        assertTrue(viewModel.state.value.canChangeOpenReservation)
        assertFalse(viewModel.state.value.canMarkOpenAsNoShow)

        viewModel.onIntent(ReservationsIntent.OpenReservation("late"))
        assertTrue(viewModel.state.value.canMarkOpenAsNoShow)
    }

    @Test
    fun markingAsNotArrivedReleasesTheNightsAndClosesTheDetail() = runTest {
        reservations.reservations.value += reservation("late", roomId = "r-101", checkIn = "2026-11-20", checkOut = "2026-11-23", roomNumber = 101, guestName = "Pedro Ruiz")
        val viewModel = viewModel()
        viewModel.onIntent(ReservationsIntent.OpenReservation("late"))

        viewModel.onIntent(ReservationsIntent.MarkNoShow)

        assertEquals(ReservationsEffect.MarkedNoShow("Pedro Ruiz"), viewModel.effects.first())
        assertNull(viewModel.state.value.openReservation)
        assertEquals(emptyList(), viewModel.state.value.today?.overdue)
    }

    @Test
    fun aCancellationNeedsItsReason() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(ReservationsIntent.OpenReservation("luis"))
        viewModel.onIntent(ReservationsIntent.StartCancel)

        viewModel.onIntent(ReservationsIntent.ConfirmCancel)
        assertEquals(ReservationError.BlankReason, viewModel.state.value.closeError)

        viewModel.onIntent(ReservationsIntent.CancelReasonChange("Cambió de planes"))
        viewModel.onIntent(ReservationsIntent.ConfirmCancel)

        assertEquals(ReservationsEffect.Cancelled("Luis Pérez"), viewModel.effects.first())
        assertEquals(listOf("ana", "marta"), viewModel.state.value.reservations.map { it.id })
    }

    @Test
    fun changingAReservationOpensItsScreen() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(ReservationsIntent.OpenReservation("luis"))

        viewModel.onIntent(ReservationsIntent.ChangeReservation)

        assertEquals(ReservationsEffect.NavigateToChange("luis"), viewModel.effects.first())
        assertNull(viewModel.state.value.openReservationId)
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
