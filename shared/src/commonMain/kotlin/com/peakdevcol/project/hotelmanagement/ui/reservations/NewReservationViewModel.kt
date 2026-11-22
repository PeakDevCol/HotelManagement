package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AvailableRoom
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.DEFAULT_NATIONALITY
import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.GuestFields
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_RESERVATION_NOTES_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_ROOM_CAPACITY
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.StayPrice
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.FindAvailableRoomsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetReservationContextUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ReservationRequest
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveGuestUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SearchGuestsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** RF-RES-03: the free rooms for the chosen days and people. */
sealed interface RoomSearch {
    /** No days chosen yet. */
    data object NotAsked : RoomSearch
    data object Searching : RoomSearch
    data class Found(val rooms: List<AvailableRoom>) : RoomSearch
    data class Failed(val error: ReservationError) : RoomSearch
}

/** RF-RES-01: the guests found by what was typed. */
sealed interface GuestSearch {
    data object Idle : GuestSearch
    data object Searching : GuestSearch
    data class Found(val guests: List<Guest>) : GuestSearch
    data class Failed(val error: GuestError) : GuestSearch
}

/** A guest being created, or completed when [existing] is not null (decision 5A). */
data class GuestForm(
    val existing: Guest? = null,
    val fullName: String = "",
    val phone: String = "",
    val documentType: DocumentType? = null,
    val documentNumber: String = "",
    val nationality: String = DEFAULT_NATIONALITY,
    val email: String = "",
    /** The field the last attempt to save found wrong. */
    val error: GuestError? = null,
) {
    fun toFields() = GuestFields(fullName, phone, documentType, documentNumber, nationality, email)

    // S9: personal data never reaches a log through the text of the object.
    override fun toString(): String = "GuestForm(existing=${existing?.id}, error=$error)"
}

data class NewReservationState(
    val isStarting: Boolean = true,
    /** The first day a reservation can take: the operational day (P17). */
    val today: CalendarDate? = null,
    /** RF-RES-02: only an administrator changes the base rate. */
    val canChangeRate: Boolean = false,
    val stay: Stay? = null,
    val isPickingDates: Boolean = false,
    val adults: Int = 1,
    val children: Int = 0,
    val roomSearch: RoomSearch = RoomSearch.NotAsked,
    val roomId: String? = null,
    val guestQuery: String = "",
    val guestSearch: GuestSearch = GuestSearch.Idle,
    val guest: Guest? = null,
    val guestForm: GuestForm? = null,
    val isSavingGuest: Boolean = false,
    val source: ReservationSource = ReservationSource.DIRECT,
    /** The rate an administrator types, in pesos. */
    val rateText: String = "",
    val notes: String = "",
    val isReserving: Boolean = false,
    /** Why the last attempt to reserve failed. */
    val error: ReservationError? = null,
) {
    val availableRooms: List<AvailableRoom> get() = (roomSearch as? RoomSearch.Found)?.rooms.orEmpty()

    val selectedRoom: AvailableRoom? get() = availableRooms.firstOrNull { it.room.id == roomId }

    /** The rate of each night: the one an administrator typed, or the base rate of the type. */
    val nightlyRate: Long? get() = selectedRoom?.let { room -> if (canChangeRate) rateText.toLongOrNull() else room.nightlyRate }

    val price: StayPrice?
        get() {
            val nights = stay?.nightCount ?: return null
            val rate = nightlyRate?.takeIf { it > 0 } ?: return null
            return StayPrice(nightlyRate = rate, nights = nights, taxRate = selectedRoom?.taxRate ?: 0)
        }

    val canReserve: Boolean get() = price != null && guest != null && !isReserving
}

sealed interface NewReservationIntent {
    data object PickDates : NewReservationIntent
    data object DismissDates : NewReservationIntent
    data class DatesChosen(val firstEpochDay: Long, val lastEpochDay: Long) : NewReservationIntent
    data class AdultsChange(val count: Int) : NewReservationIntent
    data class ChildrenChange(val count: Int) : NewReservationIntent
    data object RetryRooms : NewReservationIntent
    data class SelectRoom(val roomId: String) : NewReservationIntent
    data class GuestQueryChange(val text: String) : NewReservationIntent
    data class SelectGuest(val guestId: String) : NewReservationIntent
    data object ChangeGuest : NewReservationIntent
    data object NewGuest : NewReservationIntent
    data object EditGuest : NewReservationIntent
    data class GuestNameChange(val text: String) : NewReservationIntent
    data class GuestPhoneChange(val text: String) : NewReservationIntent
    data class DocumentTypeSelect(val type: DocumentType?) : NewReservationIntent
    data class DocumentNumberChange(val text: String) : NewReservationIntent
    data class NationalityChange(val text: String) : NewReservationIntent
    data class EmailChange(val text: String) : NewReservationIntent
    data object SaveGuest : NewReservationIntent
    data object DismissGuestForm : NewReservationIntent
    data class SourceSelect(val source: ReservationSource) : NewReservationIntent
    data class RateChange(val text: String) : NewReservationIntent
    data class NotesChange(val text: String) : NewReservationIntent
    data object Reserve : NewReservationIntent
}

sealed interface NewReservationEffect {
    data class Reserved(val guestName: String, val roomNumber: Int) : NewReservationEffect
    data class GuestFailed(val error: GuestError) : NewReservationEffect
}

/**
 * RF-RES-01 to RF-RES-03: a reservation in one screen. The days and the people find the free
 * rooms; the guest is searched, or created with the name and the phone (decision 5A).
 */
class NewReservationViewModel(
    private val getReservationContext: GetReservationContextUseCase,
    private val findAvailableRooms: FindAvailableRoomsUseCase,
    private val searchGuests: SearchGuestsUseCase,
    private val saveGuest: SaveGuestUseCase,
    private val createReservation: CreateReservationUseCase,
    /** RNF-12: each search reads up to 20 guests, so it waits for a pause in the typing. */
    private val guestSearchDelay: Duration = GUEST_SEARCH_DELAY,
) : ViewModel() {

    private val _state = MutableStateFlow(NewReservationState())
    val state: StateFlow<NewReservationState> = _state.asStateFlow()

    private val _effects = Channel<NewReservationEffect>(Channel.BUFFERED)
    val effects: Flow<NewReservationEffect> = _effects.receiveAsFlow()

    private var roomJob: Job? = null
    private var guestJob: Job? = null

    init {
        viewModelScope.launch {
            val context = getReservationContext()
            _state.update { it.copy(isStarting = false, today = context.today, canChangeRate = context.canChangeRate) }
        }
    }

    fun onIntent(intent: NewReservationIntent) {
        when (intent) {
            NewReservationIntent.PickDates -> _state.update { it.copy(isPickingDates = true) }
            NewReservationIntent.DismissDates -> _state.update { it.copy(isPickingDates = false) }
            is NewReservationIntent.DatesChosen -> chooseDates(intent.firstEpochDay, intent.lastEpochDay)
            is NewReservationIntent.AdultsChange -> changePeople { it.copy(adults = intent.count.coerceIn(1, MAX_ROOM_CAPACITY)) }
            is NewReservationIntent.ChildrenChange -> changePeople { it.copy(children = intent.count.coerceIn(0, MAX_ROOM_CAPACITY - 1)) }
            NewReservationIntent.RetryRooms -> searchRooms()
            is NewReservationIntent.SelectRoom -> selectRoom(intent.roomId)
            is NewReservationIntent.GuestQueryChange -> typeGuestQuery(intent.text)
            is NewReservationIntent.SelectGuest -> selectGuest(intent.guestId)
            NewReservationIntent.ChangeGuest -> _state.update { it.copy(guest = null, error = null) }
            NewReservationIntent.NewGuest -> _state.update { it.copy(guestForm = newGuestForm(it.guestQuery)) }
            NewReservationIntent.EditGuest -> _state.update { state -> state.copy(guestForm = state.guest?.let(::formOf)) }
            is NewReservationIntent.GuestNameChange -> editGuestForm { it.copy(fullName = intent.text) }
            is NewReservationIntent.GuestPhoneChange -> editGuestForm { it.copy(phone = intent.text) }
            is NewReservationIntent.DocumentTypeSelect -> editGuestForm { it.copy(documentType = intent.type.takeIf { type -> type != it.documentType }) }
            is NewReservationIntent.DocumentNumberChange -> editGuestForm { it.copy(documentNumber = intent.text) }
            is NewReservationIntent.NationalityChange -> editGuestForm { it.copy(nationality = intent.text) }
            is NewReservationIntent.EmailChange -> editGuestForm { it.copy(email = intent.text) }
            NewReservationIntent.SaveGuest -> saveGuestForm()
            NewReservationIntent.DismissGuestForm -> _state.update { it.copy(guestForm = null) }
            is NewReservationIntent.SourceSelect -> _state.update { it.copy(source = intent.source) }
            is NewReservationIntent.RateChange -> _state.update { it.copy(rateText = intent.text.filter(Char::isDigit).take(MAX_RATE_DIGITS), error = null) }
            is NewReservationIntent.NotesChange -> _state.update { it.copy(notes = intent.text.take(MAX_RESERVATION_NOTES_LENGTH)) }
            NewReservationIntent.Reserve -> reserve()
        }
    }

    private fun chooseDates(firstEpochDay: Long, lastEpochDay: Long) {
        val stay = Stay(CalendarDate.fromEpochDay(firstEpochDay), CalendarDate.fromEpochDay(lastEpochDay))
        _state.update { it.copy(stay = stay, isPickingDates = false, error = null) }
        searchRooms()
    }

    private fun changePeople(change: (NewReservationState) -> NewReservationState) {
        _state.update { change(it).copy(error = null) }
        if (_state.value.stay != null) searchRooms()
    }

    // A room that is no longer free, or no longer takes everyone, stops being the chosen one.
    private fun searchRooms() {
        val state = _state.value
        val stay = state.stay ?: return
        roomJob?.cancel()
        _state.update { it.copy(roomSearch = RoomSearch.Searching) }
        roomJob = viewModelScope.launch {
            val search = findAvailableRooms(stay, state.adults, state.children).fold(
                onSuccess = { RoomSearch.Found(it) },
                onFailure = { RoomSearch.Failed(it as? ReservationError ?: ReservationError.Unknown) },
            )
            _state.update { current ->
                val stillFree = (search as? RoomSearch.Found)?.rooms.orEmpty().any { it.room.id == current.roomId }
                current.copy(roomSearch = search, roomId = current.roomId.takeIf { stillFree }, rateText = current.rateText.takeIf { stillFree }.orEmpty())
            }
        }
    }

    private fun selectRoom(roomId: String) = _state.update { state ->
        val room = state.availableRooms.firstOrNull { it.room.id == roomId } ?: return@update state
        state.copy(roomId = roomId, rateText = room.nightlyRate.toString(), error = null)
    }

    private fun typeGuestQuery(text: String) {
        _state.update { it.copy(guestQuery = text) }
        guestJob?.cancel()
        guestJob = viewModelScope.launch {
            delay(guestSearchDelay)
            if (text.isBlank()) {
                _state.update { it.copy(guestSearch = GuestSearch.Idle) }
                return@launch
            }
            _state.update { it.copy(guestSearch = GuestSearch.Searching) }
            val search = searchGuests(text).fold(
                onSuccess = { GuestSearch.Found(it) },
                onFailure = { GuestSearch.Failed(it as? GuestError ?: GuestError.Unknown) },
            )
            _state.update { it.copy(guestSearch = search) }
        }
    }

    private fun selectGuest(guestId: String) = _state.update { state ->
        val found = (state.guestSearch as? GuestSearch.Found)?.guests?.firstOrNull { it.id == guestId } ?: return@update state
        state.copy(guest = found, guestQuery = "", guestSearch = GuestSearch.Idle, error = null)
    }

    // What was typed to search starts the new guest: a document if it has digits, a name otherwise.
    private fun newGuestForm(query: String): GuestForm {
        val typed = query.trim()
        return if (typed.any(Char::isDigit)) GuestForm(documentNumber = typed) else GuestForm(fullName = typed)
    }

    private fun formOf(guest: Guest) = GuestForm(
        existing = guest,
        fullName = guest.fullName,
        phone = guest.phone,
        documentType = guest.documentType,
        documentNumber = guest.documentNumber.orEmpty(),
        nationality = guest.nationality.orEmpty(),
        email = guest.email.orEmpty(),
    )

    private fun editGuestForm(change: (GuestForm) -> GuestForm) = _state.update { state ->
        state.copy(guestForm = state.guestForm?.let { change(it).copy(error = null) })
    }

    // A wrong field keeps the form open with its error; anything else closes it.
    private fun saveGuestForm() {
        val form = _state.value.guestForm ?: return
        if (_state.value.isSavingGuest) return
        _state.update { it.copy(isSavingGuest = true) }
        viewModelScope.launch {
            val result = saveGuest(form.existing, form.toFields())
            val error = result.exceptionOrNull()?.let { it as? GuestError ?: GuestError.Unknown }
            val fieldError = error.takeIf { it in GUEST_FIELD_ERRORS }
            _state.update { state ->
                state.copy(
                    isSavingGuest = false,
                    guestForm = fieldError?.let { form.copy(error = it) },
                    guest = result.getOrNull() ?: state.guest,
                    guestQuery = if (result.isSuccess) "" else state.guestQuery,
                    guestSearch = if (result.isSuccess) GuestSearch.Idle else state.guestSearch,
                    error = null,
                )
            }
            if (error != null && fieldError == null) _effects.send(NewReservationEffect.GuestFailed(error))
        }
    }

    // RF-RES-03: if someone took a night in the meantime, the free rooms are searched again.
    private fun reserve() {
        val state = _state.value
        val stay = state.stay ?: return
        if (!state.canReserve) return
        _state.update { it.copy(isReserving = true, error = null) }
        viewModelScope.launch {
            val request = ReservationRequest(
                guest = state.guest,
                roomId = state.roomId,
                stay = stay,
                adults = state.adults,
                children = state.children,
                nightlyRate = state.nightlyRate,
                source = state.source,
                notes = state.notes,
            )
            createReservation(request).fold(
                onSuccess = { reservation ->
                    _state.update { it.copy(isReserving = false) }
                    _effects.send(NewReservationEffect.Reserved(reservation.guestName, reservation.roomNumber))
                },
                onFailure = { failure ->
                    val error = failure as? ReservationError ?: ReservationError.Unknown
                    _state.update { it.copy(isReserving = false, error = error) }
                    if (error is ReservationError.NightsTaken) searchRooms()
                },
            )
        }
    }

    private companion object {
        val GUEST_SEARCH_DELAY = 400.milliseconds
        const val MAX_RATE_DIGITS = 9
        val GUEST_FIELD_ERRORS = setOf(
            GuestError.BlankName,
            GuestError.InvalidPhone,
            GuestError.IncompleteDocument,
            GuestError.DuplicateDocument,
            GuestError.InvalidEmail,
        )
    }
}
