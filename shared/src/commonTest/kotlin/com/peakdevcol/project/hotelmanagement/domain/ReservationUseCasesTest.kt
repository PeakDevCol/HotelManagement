package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.GuestFields
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.FindAvailableRoomsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetReservationContextUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveUpcomingReservationsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReservationRequest
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveGuestUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SearchGuestsUseCase
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
import kotlin.test.assertTrue

/** RF-RES-01 to RF-RES-03, with decisions 2A, 4A and 5A of M11. */
class ReservationUseCasesTest {
    // 1:00 a. m. of Nov 23 in Colombia: the operational day is still Nov 22 (P17).
    private val clock = clockAt("2026-11-23T06:00:00Z")
    private val today = day("2026-11-22")
    private val settings = FakeSettingsRepository()
    private val vat = Tax("vat", "IVA", rate = 1_900)
    private val taxes = FakeTaxRepository(vat)
    private val types = FakeRoomTypeRepository(
        roomType("double", "Doble", capacity = 2, baseRate = 119_000, taxId = "vat"),
        roomType("suite", "Suite", capacity = 4, baseRate = 600_000),
    )
    private val rooms = FakeRoomRepository(
        room("r-204", 204),
        room("r-101", 101, roomTypeId = "suite"),
        room("r-305", 305).copy(active = false),
    )
    private val reservations = FakeReservationRepository(reservation("held", roomId = "r-101", checkIn = "2026-11-23", checkOut = "2026-11-25", roomNumber = 101))
    private val users = FakeUserRepository(
        employee("admin", "Laura Méndez", UserType.ADMIN),
        employee("reception", "Carlos Gómez", UserType.RECEPTION),
        employee("ana", "Ana Rojas"),
    )
    private val ana = guest("g-ana", "Ana María Rojas")

    private fun findRooms() = FindAvailableRoomsUseCase(rooms, types, taxes, reservations, settings, clock)

    private fun createReservation(userId: String = "reception") =
        CreateReservationUseCase(FakeAuthRepository(userId), users, rooms, types, taxes, reservations, reservations, settings, clock)

    private fun request(
        checkIn: String = "2026-11-22",
        checkOut: String = "2026-11-24",
        roomId: String? = "r-204",
        adults: Int = 2,
        children: Int = 0,
        rate: Long? = null,
    ) = ReservationRequest(
        guest = ana,
        roomId = roomId,
        stay = Stay(day(checkIn), day(checkOut)),
        adults = adults,
        children = children,
        nightlyRate = rate,
        source = ReservationSource.WHATSAPP,
        notes = "  Llega tarde  ",
    )

    // ---- RF-RES-03: free rooms

    @Test
    fun theFreeRoomsAreTheActiveOnesWithEveryNightFreeAndRoomForEveryone() = runTest {
        val free = findRooms()(Stay(day("2026-11-25"), day("2026-11-27")), adults = 2, children = 0).getOrThrow()
        assertEquals(listOf(101, 204), free.map { it.room.number }, "the suite is free again from the night of the 25th")

        val held = findRooms()(Stay(day("2026-11-22"), day("2026-11-24")), adults = 2, children = 0).getOrThrow()
        assertEquals(listOf(204), held.map { it.room.number }, "the suite is held on the night of the 23rd")

        val many = findRooms()(Stay(day("2026-11-26"), day("2026-11-27")), adults = 2, children = 1).getOrThrow()
        assertEquals(listOf(101), many.map { it.room.number }, "the double takes two people, children included")
    }

    @Test
    fun aFreeRoomCarriesTheBaseRateAndTheTaxOfItsType() = runTest {
        val double = findRooms()(Stay(day("2026-11-26"), day("2026-11-27")), adults = 1, children = 0).getOrThrow().first { it.room.number == 204 }

        assertEquals(119_000, double.nightlyRate)
        assertEquals(1_900, double.taxRate)
    }

    @Test
    fun theSearchChecksTheDaysBeforeReadingAnything() = runTest {
        assertEquals(
            ReservationError.ArrivalInThePast,
            findRooms()(Stay(day("2026-11-21"), day("2026-11-23")), 1, 0).exceptionOrNull(),
        )
        assertEquals(
            ReservationError.TooManyNights,
            findRooms()(Stay(today, today.plusDays(31)), 1, 0).exceptionOrNull(),
        )
    }

    @Test
    fun withoutConnectionTheSearchSaysSo() = runTest {
        reservations.readFailure = ReservationError.NoConnection

        assertEquals(ReservationError.NoConnection, findRooms()(Stay(today, today.plusDays(1)), 1, 0).exceptionOrNull())
    }

    // ---- RF-RES-02: create

    @Test
    fun receptionReservesAtTheBaseRateAndEachNightIsHeld() = runTest {
        val made = createReservation()(request()).getOrThrow()

        assertEquals(ReservationState.CONFIRMED, made.state)
        assertEquals("Ana María Rojas", made.guestName)
        assertEquals(204, made.roomNumber)
        assertEquals(listOf(119_000L, 119_000L), made.nightlyRates)
        assertEquals(238_000, made.total)
        assertEquals(38_000, made.taxAmount)
        assertEquals("Llega tarde", made.notes)
        assertEquals("reception", made.createdBy)
        assertEquals(listOf("r-204_2026-11-22", "r-204_2026-11-23"), reservations.nights.value.filter { it.reservationId == made.id }.map { it.id })
    }

    @Test
    fun onlyAnAdministratorChangesTheRate() = runTest {
        assertEquals(ReservationError.RateNotAllowed, createReservation("reception")(request(rate = 100_000)).exceptionOrNull())

        val made = createReservation("admin")(request(rate = 100_000)).getOrThrow()
        assertEquals(listOf(100_000L, 100_000L), made.nightlyRates)
        assertEquals(ReservationError.InvalidRate, createReservation("admin")(request(checkIn = "2026-11-25", checkOut = "2026-11-26", rate = 0)).exceptionOrNull())
    }

    @Test
    fun aBasicEmployeeDoesNotReserve() = runTest {
        assertEquals(ReservationError.NotAllowed, createReservation("ana")(request()).exceptionOrNull())
        assertEquals(ReservationError.NoSession, CreateReservationUseCase(FakeAuthRepository(null), users, rooms, types, taxes, reservations, reservations, settings, clock)(request()).exceptionOrNull())
    }

    @Test
    fun theReservationNeedsAGuestAFreeActiveRoomAndRoomForEveryone() = runTest {
        assertEquals(ReservationError.MissingGuest, createReservation()(request().copy(guest = null)).exceptionOrNull())
        assertEquals(ReservationError.MissingRoom, createReservation()(request(roomId = null)).exceptionOrNull())
        assertEquals(ReservationError.MissingRoom, createReservation()(request(roomId = "r-305")).exceptionOrNull())
        assertEquals(ReservationError.OverCapacity, createReservation()(request(adults = 2, children = 1)).exceptionOrNull())
        assertEquals(ReservationError.NoAdults, createReservation()(request(adults = 0, children = 1)).exceptionOrNull())
    }

    @Test
    fun aHeldNightIsNamedAndNothingIsCreated() = runTest {
        val result = createReservation()(request(checkIn = "2026-11-22", checkOut = "2026-11-25", roomId = "r-101"))

        assertEquals(ReservationError.NightsTaken(listOf(day("2026-11-23"), day("2026-11-24"))), result.exceptionOrNull())
        assertEquals(1, reservations.reservations.value.size)
    }

    @Test
    fun twoPeopleReservingTheSameNightAtOnceNeverCross() = runTest {
        reservations.nightTakenMeanwhile = RoomNight("r-204", day("2026-11-23"), reservationId = "other-device", arrival = true)

        val result = createReservation()(request())

        assertEquals(ReservationError.NightsTaken(listOf(day("2026-11-23"))), result.exceptionOrNull())
        assertTrue(reservations.nights.value.none { it.roomId == "r-204" && it.reservationId != "other-device" })
    }

    // ---- RF-RES-01: guests

    @Test
    fun aGuestIsCreatedWithNameAndPhoneAndFoundByAnyWord() = runTest {
        val guests = FakeGuestRepository()
        val saved = SaveGuestUseCase(FakeAuthRepository("reception"), guests)(existing = null, GuestFields("Ana María Rojas", "300 123 4567")).getOrThrow()

        val found = SearchGuestsUseCase(guests)("roj ana").getOrThrow()

        assertEquals(listOf(saved), found)
        assertEquals(listOf("roj"), guests.searchedKeys, "one key, the longest word typed")
        assertEquals(emptyList(), SearchGuestsUseCase(guests)("a").getOrThrow(), "nothing is searched with one letter")
    }

    @Test
    fun aDocumentIsNotRepeatedButTheSameGuestKeepsTheirs() = runTest {
        val withDocument = ana.copy(documentType = DocumentType.CC, documentNumber = "1020304")
        val guests = FakeGuestRepository(withDocument)
        val save = SaveGuestUseCase(FakeAuthRepository("reception"), guests)

        val other = save(existing = null, GuestFields("Pedro Pérez", "3009876543", DocumentType.CC, "1.020.304"))
        assertEquals(GuestError.DuplicateDocument, other.exceptionOrNull())

        val passport = save(existing = null, GuestFields("Pedro Pérez", "3009876543", DocumentType.PASSPORT, "1020304"))
        assertTrue(passport.isSuccess, "the same number in another kind of document is another document")

        val edited = save(existing = withDocument, GuestFields("Ana María Rojas Restrepo", "3001234567", DocumentType.CC, "1020304"))
        assertEquals("Ana María Rojas Restrepo", edited.getOrThrow().fullName)
    }

    @Test
    fun withoutSessionNoGuestIsSaved() = runTest {
        assertEquals(GuestError.NoSession, SaveGuestUseCase(FakeAuthRepository(null), FakeGuestRepository())(null, GuestFields("Ana", "3001234567")).exceptionOrNull())
    }

    // ---- Decision 2A: "Próximas" and what a new reservation starts from

    @Test
    fun theUpcomingReservationsStartTodayByArrivalAndRoom() = runTest {
        val upcoming = FakeReservationRepository(
            reservation("later", roomId = "r-101", checkIn = "2026-11-30", checkOut = "2026-12-01", roomNumber = 101),
            reservation("today-204", roomId = "r-204", checkIn = "2026-11-22", checkOut = "2026-11-23", roomNumber = 204),
            reservation("today-101", roomId = "r-101", checkIn = "2026-11-22", checkOut = "2026-11-23", roomNumber = 101),
            reservation("yesterday", roomId = "r-204", checkIn = "2026-11-21", checkOut = "2026-11-22"),
        )

        val ids = ObserveUpcomingReservationsUseCase(upcoming, settings, clock)().first().map { it.id }

        assertEquals(listOf("today-101", "today-204", "later"), ids)
    }

    @Test
    fun aNewReservationStartsOnTheOperationalDayAndOnlyAnAdministratorChangesTheRate() = runTest {
        val forReception = GetReservationContextUseCase(FakeAuthRepository("reception"), users, settings, clock)()
        val forAdmin = GetReservationContextUseCase(FakeAuthRepository("admin"), users, settings, clock)()

        assertEquals(today, forReception.today)
        assertEquals(false, forReception.canChangeRate)
        assertEquals(true, forAdmin.canChangeRate)
    }
}
