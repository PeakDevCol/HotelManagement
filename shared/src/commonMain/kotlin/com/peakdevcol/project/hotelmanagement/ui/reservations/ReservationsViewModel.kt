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

/** Decision 2A of M11: the tabs of "Reservas". The calendar comes in the third PR. */
enum class ReservationsTab {
    TODAY,
    UPCOMING,
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
) {
    val arrivalDays: List<ArrivalDay>
        get() = reservations
            .filter { it.matches(query) }
            .groupBy { it.stay.checkIn }
            .map { (date, reservations) -> ArrivalDay(date, reservations) }

    val openReservation: Reservation?
        get() = openReservationId?.let { id -> today?.reservationOf(id) ?: reservations.firstOrNull { it.id == id } }

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
}

sealed interface ReservationsEffect {
    data object NavigateToNewReservation : ReservationsEffect
    data class NavigateToChange(val reservationId: String) : ReservationsEffect
    data class Cancelled(val guestName: String) : ReservationsEffect
    data class MarkedNoShow(val guestName: String) : ReservationsEffect
}

/**
 * Decision 2A of M11: "Hoy" and "Próximas", live, and the detail of a reservation, where it is
 * changed (RF-RES-05), cancelled (RF-RES-06) or marked as not arrived (RF-RES-07).
 */
class ReservationsViewModel(
    private val observeUpcomingReservations: ObserveUpcomingReservationsUseCase,
    private val observeTodayReservations: ObserveTodayReservationsUseCase,
    private val loadReservationHistory: LoadReservationHistoryUseCase,
    private val cancelReservation: CancelReservationUseCase,
    private val markNoShow: MarkNoShowUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationsState())
    val state: StateFlow<ReservationsState> = _state.asStateFlow()

    private val _effects = Channel<ReservationsEffect>(Channel.BUFFERED)
    val effects: Flow<ReservationsEffect> = _effects.receiveAsFlow()

    private var upcomingJob: Job? = null
    private var todayJob: Job? = null
    private var historyJob: Job? = null

    init {
        listenToUpcoming()
        listenToToday()
    }

    fun onIntent(intent: ReservationsIntent) {
        when (intent) {
            is ReservationsIntent.SelectTab -> _state.update { it.copy(tab = intent.tab) }
            is ReservationsIntent.QueryChange -> _state.update { it.copy(query = intent.text) }
            is ReservationsIntent.OpenReservation -> open(intent.reservationId)
            ReservationsIntent.CloseReservation -> close()
            ReservationsIntent.NewReservation -> _effects.trySend(ReservationsEffect.NavigateToNewReservation)
            ReservationsIntent.ChangeReservation -> changeOpenReservation()
            ReservationsIntent.StartCancel -> _state.update { it.copy(cancelReason = "", closeError = null) }
            is ReservationsIntent.CancelReasonChange -> _state.update { it.copy(cancelReason = intent.text, closeError = null) }
            ReservationsIntent.ConfirmCancel -> cancel()
            ReservationsIntent.DismissCancel -> _state.update { it.copy(cancelReason = null, closeError = null) }
            ReservationsIntent.MarkNoShow -> markOpenAsNoShow()
            ReservationsIntent.Retry -> {
                listenToUpcoming()
                listenToToday()
            }
        }
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
