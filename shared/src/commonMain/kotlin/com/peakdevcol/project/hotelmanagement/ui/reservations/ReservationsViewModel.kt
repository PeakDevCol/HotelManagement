package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationHistory
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.TodayReservations
import com.peakdevcol.project.hotelmanagement.domain.model.isOverdue
import com.peakdevcol.project.hotelmanagement.domain.model.matches
import com.peakdevcol.project.hotelmanagement.domain.usecase.CancelReservationUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadReservationHistoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkNoShowUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveTodayReservationsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveUpcomingReservationsUseCase
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyCalendar
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyPeriod
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancySpan
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationStart
import com.peakdevcol.project.hotelmanagement.domain.usecase.GetReservationContextUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOccupancyUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Decision 2A of M11: the tabs of "Reservas". */
enum class ReservationsTab {
    TODAY,
    UPCOMING,
    CALENDAR,
}

/** The reservations that arrive on one day. */
data class ArrivalDay(val date: CalendarDate, val reservations: List<Reservation>)

/** Decision 1A: the history of the open reservation, read when it opens. */
sealed interface HistoryLoad {
    data object Loading : HistoryLoad
    data class Loaded(val history: ReservationHistory) : HistoryLoad
    data object Failed : HistoryLoad
}

data class ReservationsState(
    val tab: ReservationsTab = ReservationsTab.TODAY,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** "Próximas": by arrival and room. */
    val reservations: List<Reservation> = emptyList(),
    /** Narrows "Próximas" by the name of the guest or the number of the room. */
    val query: String = "",
    /** "Hoy": null while it is read. */
    val today: TodayReservations? = null,
    val todayFailed: Boolean = false,
    val openReservationId: String? = null,
    val history: HistoryLoad? = null,
    /** RF-RES-06: the reason being typed, or null when the reservation is not being cancelled. */
    val cancelReason: String? = null,
    /** A cancellation or a "no llegó" waiting for the server, so it is not sent twice. */
    val isClosing: Boolean = false,
    /** Why the last cancellation or "no llegó" failed. */
    val closeError: ReservationError? = null,
    /** RF-RES-04: the days the calendar shows, null until its tab is first opened. */
    val calendarPeriod: OccupancyPeriod? = null,
    /** The calendar of [calendarPeriod], null while it is read. */
    val occupancy: OccupancyCalendar? = null,
    val occupancyFailed: Boolean = false,
    /** The operational day, so the calendar marks today and does not offer nights that are over. */
    val calendarToday: CalendarDate? = null,
) {
    val arrivalDays: List<ArrivalDay>
        get() = reservations
            .filter { it.matches(query) }
            .groupBy { it.stay.checkIn }
            .map { (date, reservations) -> ArrivalDay(date, reservations) }

    val openReservation: Reservation?
        get() = openReservationId?.let { id ->
            today?.reservationOf(id) ?: reservations.firstOrNull { it.id == id } ?: occupancy?.reservationOf(id)
        }

    /** Only a confirmed reservation is changed or cancelled; the stays of the hotel belong to M12. */
    val canChangeOpenReservation: Boolean get() = openReservation?.state == ReservationState.CONFIRMED

    /** RF-RES-07: once the operational day of its arrival is over. */
    val canMarkOpenAsNoShow: Boolean
        get() {
            val day = today?.today ?: return false
            return openReservation?.isOverdue(day) == true
        }
}

sealed interface ReservationsIntent {
    data class SelectTab(val tab: ReservationsTab) : ReservationsIntent
    data class QueryChange(val text: String) : ReservationsIntent
    data class OpenReservation(val reservationId: String) : ReservationsIntent
    data object CloseReservation : ReservationsIntent
    data object NewReservation : ReservationsIntent
    data object ChangeReservation : ReservationsIntent
    data object StartCancel : ReservationsIntent
    data class CancelReasonChange(val text: String) : ReservationsIntent
    data object ConfirmCancel : ReservationsIntent
    data object DismissCancel : ReservationsIntent
    data object MarkNoShow : ReservationsIntent
    data object Retry : ReservationsIntent

    /** RF-RES-04. */
    data class SelectSpan(val span: OccupancySpan) : ReservationsIntent
    data object PreviousPeriod : ReservationsIntent
    data object NextPeriod : ReservationsIntent
    data object CalendarToday : ReservationsIntent

    /** Decision 1A of the third PR of M11: a free night of a room starts a reservation there. */
    data class ReserveNight(val roomId: String, val night: CalendarDate) : ReservationsIntent
}

sealed interface ReservationsEffect {
    /** A null [start] is a reservation from nothing. */
    data class NavigateToNewReservation(val start: ReservationStart? = null) : ReservationsEffect
    data class NavigateToChange(val reservationId: String) : ReservationsEffect
    data class Cancelled(val guestName: String) : ReservationsEffect
    data class MarkedNoShow(val guestName: String) : ReservationsEffect
}

/**
 * Decision 2A of M11: "Hoy", "Próximas" and the calendar (RF-RES-04), live, and the detail of a
 * reservation, where it is changed (RF-RES-05), cancelled (RF-RES-06) or marked as not arrived (RF-RES-07).
 */
class ReservationsViewModel(
    private val observeUpcomingReservations: ObserveUpcomingReservationsUseCase,
    private val observeTodayReservations: ObserveTodayReservationsUseCase,
    private val loadReservationHistory: LoadReservationHistoryUseCase,
    private val cancelReservation: CancelReservationUseCase,
    private val markNoShow: MarkNoShowUseCase,
    private val observeOccupancy: ObserveOccupancyUseCase,
    private val getReservationContext: GetReservationContextUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationsState())
    val state: StateFlow<ReservationsState> = _state.asStateFlow()

    private val _effects = Channel<ReservationsEffect>(Channel.BUFFERED)
    val effects: Flow<ReservationsEffect> = _effects.receiveAsFlow()

    private var upcomingJob: Job? = null
    private var todayJob: Job? = null
    private var historyJob: Job? = null
    private var occupancyJob: Job? = null

    init {
        listenToUpcoming()
        listenToToday()
    }

    fun onIntent(intent: ReservationsIntent) {
        when (intent) {
            is ReservationsIntent.SelectTab -> selectTab(intent.tab)
            is ReservationsIntent.QueryChange -> _state.update { it.copy(query = intent.text) }
            is ReservationsIntent.OpenReservation -> open(intent.reservationId)
            ReservationsIntent.CloseReservation -> close()
            ReservationsIntent.NewReservation -> _effects.trySend(ReservationsEffect.NavigateToNewReservation())
            ReservationsIntent.ChangeReservation -> changeOpenReservation()
            ReservationsIntent.StartCancel -> _state.update { it.copy(cancelReason = "", closeError = null) }
            is ReservationsIntent.CancelReasonChange -> _state.update { it.copy(cancelReason = intent.text, closeError = null) }
            ReservationsIntent.ConfirmCancel -> cancel()
            ReservationsIntent.DismissCancel -> _state.update { it.copy(cancelReason = null, closeError = null) }
            ReservationsIntent.MarkNoShow -> markOpenAsNoShow()
            ReservationsIntent.Retry -> {
                listenToUpcoming()
                listenToToday()
                _state.value.calendarPeriod?.let(::listenToOccupancy)
            }
            is ReservationsIntent.SelectSpan -> changePeriod { period, today -> period.inSpan(intent.span, today) }
            ReservationsIntent.PreviousPeriod -> changePeriod { period, _ -> period.previous() }
            ReservationsIntent.NextPeriod -> changePeriod { period, _ -> period.next() }
            ReservationsIntent.CalendarToday -> changePeriod { period, today -> OccupancyPeriod.from(today, period.span) }
            is ReservationsIntent.ReserveNight -> reserveNight(intent.roomId, intent.night)
        }
    }

    // RF-RES-04: the calendar is read only once its tab is opened (RNF-12), from two weeks from today (decision 2A).
    private fun selectTab(tab: ReservationsTab) {
        _state.update { it.copy(tab = tab) }
        if (tab != ReservationsTab.CALENDAR || _state.value.calendarPeriod != null) return
        viewModelScope.launch {
            val today = getReservationContext().today
            val period = OccupancyPeriod.from(today)
            _state.update { it.copy(calendarToday = today, calendarPeriod = period) }
            listenToOccupancy(period)
        }
    }

    private fun changePeriod(change: (OccupancyPeriod, CalendarDate) -> OccupancyPeriod) {
        val state = _state.value
        val period = state.calendarPeriod ?: return
        val today = state.calendarToday ?: return
        val next = change(period, today)
        if (next == period) return
        _state.update { it.copy(calendarPeriod = next, occupancy = null) }
        listenToOccupancy(next)
    }

    private fun listenToOccupancy(period: OccupancyPeriod) {
        occupancyJob?.cancel()
        _state.update { it.copy(occupancyFailed = false) }
        occupancyJob = viewModelScope.launch {
            observeOccupancy(period)
                .catch { _state.update { it.copy(occupancyFailed = true) } }
                .collect { calendar -> _state.update { it.copy(occupancy = calendar) } }
        }
    }

    // Only a night that is not over; the new reservation checks again that it is free.
    private fun reserveNight(roomId: String, night: CalendarDate) {
        val today = _state.value.calendarToday ?: return
        if (night < today) return
        _effects.trySend(ReservationsEffect.NavigateToNewReservation(ReservationStart(roomId, night)))
    }

    private fun listenToUpcoming() {
        upcomingJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        upcomingJob = viewModelScope.launch {
            observeUpcomingReservations()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { reservations -> _state.update { it.copy(isLoading = false, reservations = reservations) } }
        }
    }

    private fun listenToToday() {
        todayJob?.cancel()
        _state.update { it.copy(todayFailed = false) }
        todayJob = viewModelScope.launch {
            observeTodayReservations()
                .catch { _state.update { it.copy(todayFailed = true) } }
                .collect { today -> _state.update { it.copy(today = today) } }
        }
    }

    private fun open(reservationId: String) {
        _state.update { it.copy(openReservationId = reservationId, history = HistoryLoad.Loading, cancelReason = null, closeError = null) }
        val reservation = _state.value.openReservation ?: return
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val history = loadReservationHistory(reservation).fold(onSuccess = { HistoryLoad.Loaded(it) }, onFailure = { HistoryLoad.Failed })
            _state.update { state -> if (state.openReservationId == reservationId) state.copy(history = history) else state }
        }
    }

    private fun close() {
        historyJob?.cancel()
        _state.update { it.copy(openReservationId = null, history = null, cancelReason = null, closeError = null) }
    }

    private fun changeOpenReservation() {
        val state = _state.value
        val reservation = state.openReservation ?: return
        if (!state.canChangeOpenReservation) return
        close()
        _effects.trySend(ReservationsEffect.NavigateToChange(reservation.id))
    }

    private fun cancel() {
        val state = _state.value
        val reservation = state.openReservation ?: return
        val reason = state.cancelReason ?: return
        closeReservation(ReservationsEffect.Cancelled(reservation.guestName)) { cancelReservation(reservation, reason) }
    }

    private fun markOpenAsNoShow() {
        val state = _state.value
        val reservation = state.openReservation ?: return
        if (!state.canMarkOpenAsNoShow) return
        closeReservation(ReservationsEffect.MarkedNoShow(reservation.guestName)) { markNoShow(reservation) }
    }

    // The detail closes once done; on a failure it stays open with the reason.
    private fun closeReservation(done: ReservationsEffect, action: suspend () -> Result<Unit>) {
        if (_state.value.isClosing) return
        _state.update { it.copy(isClosing = true, closeError = null) }
        viewModelScope.launch {
            val error = action().exceptionOrNull()?.let { it as? ReservationError ?: ReservationError.Unknown }
            _state.update { it.copy(isClosing = false, closeError = error) }
            if (error == null) {
                close()
                _effects.send(done)
            }
        }
    }
}
