package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.FindAvailableRoomsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetReservationContextUseCase
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
import com.peakdevcol.project.hotelmanagement.fakes.room
import com.peakdevcol.project.hotelmanagement.fakes.roomType
import com.peakdevcol.project.hotelmanagement.ui.reservations.GuestSearch
import com.peakdevcol.project.hotelmanagement.ui.reservations.NewReservationEffect
import com.peakdevcol.project.hotelmanagement.ui.reservations.NewReservationIntent
import com.peakdevcol.project.hotelmanagement.ui.reservations.NewReservationViewModel
import com.peakdevcol.project.hotelmanagement.ui.reservations.RoomSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

/** RF-RES-01 to RF-RES-03 in one screen, with decision 5A of M11. */
class NewReservationViewModelTest : MainDispatcherTest() {
    private val clock = clockAt("2026-11-22T15:00:00Z")
    private val settings = FakeSettingsRepository()
    private val taxes = FakeTaxRepository(Tax("vat", "IVA", rate = 1_900))
    private val types = FakeRoomTypeRepository(roomType("double", "Doble", capacity = 2, baseRate = 119_000, taxId = "vat"), roomType("suite", "Suite", capacity = 4, baseRate = 600_000))
    private val rooms = FakeRoomRepository(room("r-204", 204), room("r-101", 101, roomTypeId = "suite"))
    private val reservations = FakeReservationRepository()
    private val ana = guest("g-ana", "Ana María Rojas").copy(documentType = DocumentType.CC, documentNumber = "1020304")
    private val guests = FakeGuestRepository(ana)
    private val users = FakeUserRepository(employee("admin", "Laura Méndez", UserType.ADMIN), employee("reception", "Carlos Gómez", UserType.RECEPTION))

    // 2026-11-22 and 2026-11-24, as the date picker gives them.
    private val nov22 = day("2026-11-22").epochDay
    private val nov24 = day("2026-11-24").epochDay

    private fun viewModel(userId: String = "reception"): NewReservationViewModel {
        val auth = FakeAuthRepository(userId)
        return NewReservationViewModel(
            getReservationContext = GetReservationContextUseCase(auth, users, settings, clock),
            findAvailableRooms = FindAvailableRoomsUseCase(rooms, types, taxes, reservations, settings, clock),
            searchGuests = SearchGuestsUseCase(guests),
            saveGuest = SaveGuestUseCase(auth, guests),
            createReservation = CreateReservationUseCase(auth, users, rooms, types, taxes, reservations, reservations, settings, clock),
            guestSearchDelay = Duration.ZERO,
        )
    }

    private fun NewReservationViewModel.chooseStayRoomAndGuest() {
        onIntent(NewReservationIntent.DatesChosen(nov22, nov24))
        onIntent(NewReservationIntent.SelectRoom("r-204"))
        onIntent(NewReservationIntent.GuestQueryChange("ana"))
        onIntent(NewReservationIntent.SelectGuest("g-ana"))
    }

    @Test
    fun itStartsOnTheOperationalDayWithoutDaysChosen() {
        val state = viewModel().state.value

        assertFalse(state.isStarting)
        assertEquals(day("2026-11-22"), state.today)
        assertNull(state.stay)
        assertEquals(RoomSearch.NotAsked, state.roomSearch)
    }

    @Test
    fun choosingTheDaysFindsTheFreeRoomsAndMorePeopleNarrowThem() {
        val viewModel = viewModel()

        viewModel.onIntent(NewReservationIntent.DatesChosen(nov22, nov24))
        assertEquals(Stay(day("2026-11-22"), day("2026-11-24")), viewModel.state.value.stay)
        assertEquals(listOf(101, 204), viewModel.state.value.availableRooms.map { it.room.number })

        viewModel.onIntent(NewReservationIntent.SelectRoom("r-204"))
        viewModel.onIntent(NewReservationIntent.AdultsChange(3))

        assertEquals(listOf(101), viewModel.state.value.availableRooms.map { it.room.number })
        assertNull(viewModel.state.value.roomId, "the double no longer takes everyone")
    }

    @Test
    fun theSummaryHasTheNightsTheTaxAndTheTotal() {
        val viewModel = viewModel()

        viewModel.chooseStayRoomAndGuest()

        val price = viewModel.state.value.price!!
        assertEquals(238_000, price.total)
        assertEquals(38_000, price.taxAmount)
        assertTrue(viewModel.state.value.canReserve)
    }

    @Test
    fun receptionReservesAtTheBaseRateAndTheScreenSaysSo() = runTest {
        val viewModel = viewModel()
        viewModel.chooseStayRoomAndGuest()
        viewModel.onIntent(NewReservationIntent.SourceSelect(ReservationSource.WHATSAPP))
        viewModel.onIntent(NewReservationIntent.RateChange("100000"))

        viewModel.onIntent(NewReservationIntent.Reserve)

        assertEquals(NewReservationEffect.Reserved("Ana María Rojas", 204), viewModel.effects.first())
        val made = reservations.reservations.value.single()
        assertEquals(listOf(119_000L, 119_000L), made.nightlyRates, "the rate typed is only taken from an administrator")
        assertEquals(ReservationSource.WHATSAPP, made.source)
    }

    @Test
    fun anAdministratorReservesAtTheRateTyped() = runTest {
        val viewModel = viewModel("admin")
        viewModel.chooseStayRoomAndGuest()
        assertEquals("119000", viewModel.state.value.rateText)

        viewModel.onIntent(NewReservationIntent.RateChange("$ 100.000"))
        viewModel.onIntent(NewReservationIntent.Reserve)

        assertIs<NewReservationEffect.Reserved>(viewModel.effects.first())
        assertEquals(listOf(100_000L, 100_000L), reservations.reservations.value.single().nightlyRates)
    }

    @Test
    fun aNightTakenMeanwhileIsNamedAndTheRoomsAreSearchedAgain() {
        val viewModel = viewModel()
        viewModel.chooseStayRoomAndGuest()
        reservations.nightTakenMeanwhile = RoomNight("r-204", day("2026-11-23"), reservationId = "other", arrival = true)

        viewModel.onIntent(NewReservationIntent.Reserve)

        val state = viewModel.state.value
        assertEquals(ReservationError.NightsTaken(listOf(day("2026-11-23"))), state.error)
        assertEquals(listOf(101), state.availableRooms.map { it.room.number })
        assertNull(state.roomId)
        assertFalse(state.canReserve)
    }

    @Test
    fun aGuestIsFoundByNameOrDocument() {
        val viewModel = viewModel()

        viewModel.onIntent(NewReservationIntent.GuestQueryChange("1.020.304"))
        assertEquals(listOf("g-ana"), (viewModel.state.value.guestSearch as GuestSearch.Found).guests.map { it.id })

        viewModel.onIntent(NewReservationIntent.GuestQueryChange(""))
        assertEquals(GuestSearch.Idle, viewModel.state.value.guestSearch)
    }

    @Test
    fun aNewGuestStartsFromWhatWasTypedAndIsChosenOnceSaved() {
        val viewModel = viewModel()
        viewModel.onIntent(NewReservationIntent.GuestQueryChange("Pedro Pérez"))

        viewModel.onIntent(NewReservationIntent.NewGuest)
        assertEquals("Pedro Pérez", viewModel.state.value.guestForm?.fullName)

        viewModel.onIntent(NewReservationIntent.SaveGuest)
        assertEquals(GuestError.InvalidPhone, viewModel.state.value.guestForm?.error)

        viewModel.onIntent(NewReservationIntent.GuestPhoneChange("310 555 1234"))
        viewModel.onIntent(NewReservationIntent.SaveGuest)

        val state = viewModel.state.value
        assertNull(state.guestForm)
        assertEquals("Pedro Pérez", state.guest?.fullName)
        assertEquals("", state.guestQuery)
    }

    @Test
    fun aDocumentTypedToSearchStartsTheNewGuestWithIt() {
        val viewModel = viewModel()
        viewModel.onIntent(NewReservationIntent.GuestQueryChange("80123456"))

        viewModel.onIntent(NewReservationIntent.NewGuest)

        assertEquals("80123456", viewModel.state.value.guestForm?.documentNumber)
        assertEquals("", viewModel.state.value.guestForm?.fullName)
    }

    @Test
    fun theDataOfTheChosenGuestIsCompletedAndARepeatedDocumentIsSaid() {
        guests.guests.value += guest("g-luis", "Luis Pérez").copy(documentType = DocumentType.CC, documentNumber = "7654321")
        val viewModel = viewModel()
        viewModel.chooseStayRoomAndGuest()

        viewModel.onIntent(NewReservationIntent.EditGuest)
        viewModel.onIntent(NewReservationIntent.DocumentNumberChange("7654321"))
        viewModel.onIntent(NewReservationIntent.SaveGuest)
        assertEquals(GuestError.DuplicateDocument, viewModel.state.value.guestForm?.error)

        viewModel.onIntent(NewReservationIntent.DocumentNumberChange("1020304"))
        viewModel.onIntent(NewReservationIntent.EmailChange("ana@correo.com"))
        viewModel.onIntent(NewReservationIntent.SaveGuest)

        assertNull(viewModel.state.value.guestForm)
        assertEquals("ana@correo.com", viewModel.state.value.guest?.email)
    }

    @Test
    fun choosingTheSameDocumentTypeAgainLeavesItEmpty() {
        val viewModel = viewModel()
        viewModel.onIntent(NewReservationIntent.NewGuest)

        viewModel.onIntent(NewReservationIntent.DocumentTypeSelect(DocumentType.CC))
        viewModel.onIntent(NewReservationIntent.DocumentTypeSelect(DocumentType.CC))

        assertNull(viewModel.state.value.guestForm?.documentType)
    }
}
