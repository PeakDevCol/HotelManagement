package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.matches
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

/** The reservations that arrive on one day. */
data class ArrivalDay(val date: CalendarDate, val reservations: List<Reservation>)

data class ReservationsState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** By arrival and room. */
    val reservations: List<Reservation> = emptyList(),
    /** Narrows the list by the name of the guest or the number of the room. */
    val query: String = "",
    val openReservationId: String? = null,
) {
    val arrivalDays: List<ArrivalDay>
        get() = reservations
            .filter { it.matches(query) }
            .groupBy { it.stay.checkIn }
            .map { (date, reservations) -> ArrivalDay(date, reservations) }

    val openReservation: Reservation? get() = reservations.firstOrNull { it.id == openReservationId }
}

sealed interface ReservationsIntent {
    data class QueryChange(val text: String) : ReservationsIntent
    data class OpenReservation(val reservationId: String) : ReservationsIntent
    data object CloseReservation : ReservationsIntent
    data object NewReservation : ReservationsIntent
    data object Retry : ReservationsIntent
}

sealed interface ReservationsEffect {
    data object NavigateToNewReservation : ReservationsEffect
}

/** Decision 2A of M11: "Próximas", the confirmed reservations from today on, live. */
class ReservationsViewModel(
    private val observeUpcomingReservations: ObserveUpcomingReservationsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationsState())
    val state: StateFlow<ReservationsState> = _state.asStateFlow()

    private val _effects = Channel<ReservationsEffect>(Channel.BUFFERED)
    val effects: Flow<ReservationsEffect> = _effects.receiveAsFlow()

    private var listJob: Job? = null

    init {
        listenToReservations()
    }

    fun onIntent(intent: ReservationsIntent) {
        when (intent) {
            is ReservationsIntent.QueryChange -> _state.update { it.copy(query = intent.text) }
            is ReservationsIntent.OpenReservation -> _state.update { it.copy(openReservationId = intent.reservationId) }
            ReservationsIntent.CloseReservation -> _state.update { it.copy(openReservationId = null) }
            ReservationsIntent.NewReservation -> _effects.trySend(ReservationsEffect.NavigateToNewReservation)
            ReservationsIntent.Retry -> listenToReservations()
        }
    }

    private fun listenToReservations() {
        listJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        listJob = viewModelScope.launch {
            observeUpcomingReservations()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { reservations -> _state.update { it.copy(isLoading = false, reservations = reservations) } }
        }
    }
}
