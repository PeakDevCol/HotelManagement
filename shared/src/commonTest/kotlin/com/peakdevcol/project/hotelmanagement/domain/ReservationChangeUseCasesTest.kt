package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.FindAvailableRoomsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadReservationHistoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadReservationToChangeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkNoShowUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ModifyReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveTodayReservationsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReservationChangeRequest
import com.peakdevcol.project.hotelmanagement.domain.usecase.StayPlanner
import com.peakdevcol.project.hotelmanagement.domain.usecase.StayRequest
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeGuestRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeReservationRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomTypeRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeTaxRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.clockAt
import com.peakdevcol.project.hotelmanagement.fakes.day
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.guest
import com.peakdevcol.project.hotelmanagement.fakes.reservation
import com.peakdevcol.project.hotelmanagement.fakes.room
import com.peakdevcol.project.hotelmanagement.fakes.roomType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF-RES-05 to RF-RES-07 and RF-RES-09, with decisions 1A and 2A of the second PR of M11 and 3A. */
class ReservationChangeUseCasesTest {
    // 10:00 a. m. of Nov 22 in Colombia.
    private val clock = clockAt("2026-11-22T15:00:00Z")
    private val settings = FakeSettingsRepository()
    private val taxes = FakeTaxRepository(Tax("vat", "IVA", rate = 1_900))
    private val types = FakeRoomTypeRepository(
        roomType("double", "Doble", capacity = 2, baseRate = 119_000, taxId = "vat"),
        roomType("suite", "Suite", capacity = 4, baseRate = 600_000),
    )
    private val rooms = FakeRoomRepository(
        room("r-204", 204),
        room("r-205", 205).copy(housekeeping = Housekeeping.DIRTY),
        room("r-101", 101, roomTypeId = "suite"),
    )
    // Ana got a special rate from an administrator in the double 204.
    private val ana = reservation("ana", roomId = "r-204", checkIn = "2026-11-22", checkOut = "2026-11-24", roomNumber = 204, rate = 100_000)
    private val luis = reservation("luis", roomId = "r-205", checkIn = "2026-11-25", checkOut = "2026-11-27", roomNumber = 205, guestName = "Luis Pérez")
    private val late = reservation("late", roomId = "r-101", checkIn = "2026-11-21", checkOut = "2026-11-23", roomNumber = 101, guestName = "Marta Gómez")
    private val reservations = FakeReservationRepository(ana, luis, late)
    private val users = FakeUserRepository(
        employee("admin", "Laura Méndez", UserType.ADMIN),
        employee("reception", "Carlos Gómez", UserType.RECEPTION),
        employee("ana-basic", "Ana Basic"),
    )
    private val planner = StayPlanner(rooms, types, taxes, reservations, settings, clock)

    private fun modify(userId: String = "reception") = ModifyReservationUseCase(FakeAuthRepository(userId), users, planner, reservations)

    private fun change(roomId: String = "r-204", checkIn: String = "2026-11-22", checkOut: String = "2026-11-25", adults: Int = 2, rate: Long? = null) =
        ReservationChangeRequest(StayRequest(roomId, Stay(day(checkIn), day(checkOut)), adults, children = 0, nightlyRate = rate), ReservationSource.PHONE, notes = "")

    // ---- RF-RES-05

    @Test
    fun anotherNightInTheSameTypeKeepsTheRateOfTheReservation() = runTest {
        val updated = modify()(ana, change(checkOut = "2026-11-25")).getOrThrow()

        assertEquals(listOf(100_000L, 100_000L, 100_000L), updated.nightlyRates)
        assertEquals(
            listOf("r-204_2026-11-22", "r-204_2026-11-23", "r-204_2026-11-24"),
            reservations.nights.value.filter { it.reservationId == "ana" }.map { it.id },
        )
    }

    @Test
    fun anotherTypeTakesItsBaseRateAndOnlyAnAdministratorChangesIt() = runTest {
        val suite = modify()(ana, change(roomId = "r-101", checkIn = "2026-11-22", checkOut = "2026-11-24")).exceptionOrNull()
        assertEquals(ReservationError.NightsTaken(listOf(day("2026-11-22"))), suite, "the suite is held by Marta that night")

        val moved = modify()(ana, change(roomId = "r-101", checkIn = "2026-11-24", checkOut = "2026-11-26")).getOrThrow()
        assertEquals(listOf(600_000L, 600_000L), moved.nightlyRates)
        assertEquals(101, moved.roomNumber)
        assertTrue(reservations.nights.value.none { it.roomId == "r-204" && it.reservationId == "ana" }, "the nights of the 204 are released")

        val keptByReception = modify()(moved, change(roomId = "r-101", checkIn = "2026-11-24", checkOut = "2026-11-26", rate = 500_000)).exceptionOrNull()
        assertEquals(ReservationError.RateNotAllowed, keptByReception)
        assertEquals(listOf(500_000L, 500_000L), modify("admin")(moved, change(roomId = "r-101", checkIn = "2026-11-24", checkOut = "2026-11-26", rate = 500_000)).getOrThrow().nightlyRates)
    }

    @Test
    fun theFirstNightIsStillTheArrivalAfterMovingTheDays() = runTest {
        modify()(ana, change(checkIn = "2026-11-23", checkOut = "2026-11-25")).getOrThrow()

        val nights = reservations.nights.value.filter { it.reservationId == "ana" }
        assertEquals(listOf(true, false), nights.sortedBy { it.date }.map { it.arrival })
        assertEquals(day("2026-11-23"), nights.single { it.arrival }.date)
    }

    @Test
    fun theHistoryKeepsWhatItWasAndWhatItBecame() = runTest {
        modify()(ana, change(adults = 1)).getOrThrow()

        val event = reservations.events.getValue("ana").single()
        assertEquals(ReservationEventType.MODIFIED, event.type)
        assertEquals("reception", event.by)
        assertEquals(2, event.before?.adults)
        assertEquals(1, event.after?.adults)
        assertEquals(200_000, event.before?.total)
        assertEquals(300_000, event.after?.total)
    }

    @Test
    fun aReservationChangedMeanwhileIsNotOverwritten() = runTest {
        val seen = ana
        modify("admin")(ana, change(adults = 1)).getOrThrow()

        assertEquals(ReservationError.Changed, modify()(seen, change(checkOut = "2026-11-26")).exceptionOrNull())
    }

    @Test
    fun aNightTakenMeanwhileIsNamed() = runTest {
        reservations.nightTakenMeanwhile = RoomNight("r-204", day("2026-11-24"), reservationId = "other", arrival = true)

        assertEquals(ReservationError.NightsTaken(listOf(day("2026-11-24"))), modify()(ana, change(checkOut = "2026-11-25")).exceptionOrNull())
    }

    @Test
    fun theFreeRoomsOfAChangeCountItsOwnNightsAndTakeItsRate() = runTest {
        val free = FindAvailableRoomsUseCase(rooms, types, taxes, reservations, settings, clock)(Stay(day("2026-11-23"), day("2026-11-24")), 2, 0, changing = ana).getOrThrow()

        assertEquals(listOf(101, 204, 205), free.map { it.room.number }, "Ana's own night in the 204 counts as free")
        assertEquals(100_000, free.first { it.room.id == "r-204" }.nightlyRate, "the same type keeps the rate of the reservation")
        assertEquals(100_000, free.first { it.room.id == "r-205" }.nightlyRate, "another double too")
        assertEquals(600_000, free.first { it.room.id == "r-101" }.nightlyRate, "another type takes its base rate")
    }

    @Test
    fun aBasicEmployeeDoesNotChangeReservations() = runTest {
        assertEquals(ReservationError.NotAllowed, modify("ana-basic")(ana, change(adults = 1)).exceptionOrNull())
        assertEquals(ReservationError.NotAllowed, CancelReservationUseCase(FakeAuthRepository("ana-basic"), users, reservations)(ana, "No viene").exceptionOrNull())
    }

    // ---- RF-RES-06

    @Test
    fun aCancellationKeepsItsReasonAndReleasesEveryNight() = runTest {
        val cancel = CancelReservationUseCase(FakeAuthRepository("reception"), users, reservations)

        assertEquals(ReservationError.BlankReason, cancel(luis, "   ").exceptionOrNull())
        cancel(luis, "  Cambió de planes  ").getOrThrow()

        assertEquals(ReservationState.CANCELLED, reservations.reservations.value.first { it.id == "luis" }.state)
        assertTrue(reservations.nights.value.none { it.reservationId == "luis" })
        assertEquals("Cambió de planes", reservations.events.getValue("luis").single().reason)
        assertEquals(ReservationError.Changed, cancel(luis, "Otra vez").exceptionOrNull())
    }

    // ---- RF-RES-07

    @Test
    fun onlyAReservationWhoseDayIsOverIsMarkedAsNotArrived() = runTest {
        val markNoShow = MarkNoShowUseCase(FakeAuthRepository("reception"), users, reservations, settings, clock)

        assertEquals(ReservationError.NotOverdue, markNoShow(ana).exceptionOrNull(), "Ana arrives today")
        markNoShow(late).getOrThrow()

        assertEquals(ReservationState.NO_SHOW, reservations.reservations.value.first { it.id == "late" }.state)
        assertTrue(reservations.nights.value.none { it.reservationId == "late" })
        assertEquals(ReservationEventType.NO_SHOW, reservations.events.getValue("late").single().type)
    }

    @Test
    fun aGuestWhoArrivesAfterMidnightIsNotOverdueBeforeTheOperationalDayEnds() = runTest {
        // 1:00 a. m. of Nov 22: the operational day is still Nov 21, the day of Marta's arrival.
        val beforeDayEnds = MarkNoShowUseCase(FakeAuthRepository("reception"), users, reservations, settings, clockAt("2026-11-22T06:00:00Z"))

        assertEquals(ReservationError.NotOverdue, beforeDayEnds(late).exceptionOrNull())
    }

    // ---- RF-RES-09

    @Test
    fun todayHasTheArrivalsWithTheirRoomTheDeparturesAndTheOverdue() = runTest {
        val inHouse = reservation("in", roomId = "r-205", checkIn = "2026-11-20", checkOut = "2026-11-22", roomNumber = 205).copy(state = ReservationState.CHECKED_IN)
        reservations.reservations.value += inHouse

        val today = ObserveTodayReservationsUseCase(reservations, rooms, settings, clock)().first()

        assertEquals(day("2026-11-22"), today.today)
        assertEquals(listOf("ana"), today.arrivals.map { it.reservation.id })
        assertEquals(RoomStatus.AVAILABLE, today.arrivals.single().roomStatus)
        assertEquals(listOf("in"), today.departures.map { it.id })
        assertEquals(listOf("late"), today.overdue.map { it.id })
    }

    // ---- Opening a change and the history

    @Test
    fun aChangeStartsFromTheReservationAndItsGuestAsTheServerHasThem() = runTest {
        val guests = FakeGuestRepository(guest("guest-ana", "Ana María Rojas"))
        val load = LoadReservationToChangeUseCase(reservations, guests)

        val loaded = load("ana").getOrThrow()
        assertEquals("Ana María Rojas", loaded.guest.fullName)

        CancelReservationUseCase(FakeAuthRepository("reception"), users, reservations)(ana, "No viene").getOrThrow()
        assertEquals(ReservationError.Changed, load("ana").exceptionOrNull())
    }

    @Test
    fun theHistorySaysWhoMadeEachChange() = runTest {
        CancelReservationUseCase(FakeAuthRepository("reception"), users, reservations)(luis, "Cambió de planes").getOrThrow()

        val history = LoadReservationHistoryUseCase(reservations, users)("luis").getOrThrow()

        assertEquals("Carlos Gómez", history.single().byName)
        assertNull(LoadReservationHistoryUseCase(reservations, users)("ana").getOrThrow().firstOrNull())
    }
}
