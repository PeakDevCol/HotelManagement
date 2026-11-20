package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkOutcome
import com.peakdevcol.project.hotelmanagement.domain.model.OpenEntry
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftsOfDay
import com.peakdevcol.project.hotelmanagement.domain.model.byStartDay
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccess
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkWithPhoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveMyScheduleUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntryUseCase
import com.peakdevcol.project.hotelmanagement.ui.currentorders.everyMinute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class AttendanceState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** RF-ASI-02: the shifts of the next two weeks, by day. */
    val days: List<ShiftsOfDay> = emptyList(),
    /** To say "Hoy" and "Mañana" instead of the date. */
    val today: CalendarDate? = null,
    /** Null where marking needs no location of this device, like the desktop app. */
    val marking: PhoneMarking? = null,
    /** S10: the app explains what the location is for before the system asks. */
    val isExplainingPermission: Boolean = false,
)

/** RF-ASI-03 to RF-ASI-05 from the phone of the employee. */
data class PhoneMarking(
    val now: Instant,
    val isLoading: Boolean = true,
    val openEntry: OpenEntry? = null,
    val isMarking: Boolean = false,
    val result: MarkResult? = null,
) {
    /** The entry the next mark closes. */
    val currentEntry: AttendanceRecord? get() = openEntry?.takeUnless { it.isForgotten(now) }?.record

    /** RF-ASI-06: an entry left without exit, which the administrator closes. */
    val forgottenEntry: AttendanceRecord? get() = openEntry?.takeIf { it.isForgotten(now) }?.record

    val nextMark: MarkKind get() = if (currentEntry == null) MarkKind.ENTRY else MarkKind.EXIT
}

/** What the last mark did, shown until the next one. */
sealed interface MarkResult {
    data class Done(val outcome: MarkOutcome) : MarkResult
    data class Failed(val kind: MarkKind, val error: AttendanceError) : MarkResult
}

sealed interface AttendanceIntent {
    data object Retry : AttendanceIntent

    /** The entry or the exit, whichever comes next. */
    data object Mark : AttendanceIntent
    data class PermissionResult(val granted: Boolean) : AttendanceIntent
    data object DismissPermission : AttendanceIntent
    data object DismissResult : AttendanceIntent
}

/** RF-ASI-02 to RF-ASI-05: the shifts of the employee, and marking their entry and exit. */
class AttendanceViewModel(
    private val observeMySchedule: ObserveMyScheduleUseCase,
    private val observeOpenEntry: ObserveOpenEntryUseCase,
    private val markWithPhone: MarkWithPhoneUseCase,
    private val locationAccess: LocationAccessUseCase,
    private val clock: Clock = Clock.System,
    minuteTicks: Flow<Instant> = everyMinute(clock),
) : ViewModel() {

    private val _state = MutableStateFlow(
        AttendanceState(marking = if (locationAccess() == LocationAccess.UNAVAILABLE) null else PhoneMarking(now = clock.now())),
    )
    val state: StateFlow<AttendanceState> = _state.asStateFlow()

    private var scheduleJob: Job? = null

    init {
        listenToSchedule()
        if (_state.value.marking != null) {
            listenToOpenEntry()
            // "Llevas 3 h 15 min" stays right, and a forgotten entry stops being the one to close.
            viewModelScope.launch { minuteTicks.collect { now -> updateMarking { it.copy(now = now) } } }
        }
    }

    fun onIntent(intent: AttendanceIntent) {
        when (intent) {
            AttendanceIntent.Retry -> listenToSchedule()
            AttendanceIntent.Mark -> askToMark()
            is AttendanceIntent.PermissionResult -> {
                _state.update { it.copy(isExplainingPermission = false) }
                if (intent.granted) mark() else fail(AttendanceError.LocationPermissionDenied)
            }
            AttendanceIntent.DismissPermission -> _state.update { it.copy(isExplainingPermission = false) }
            AttendanceIntent.DismissResult -> updateMarking { it.copy(result = null) }
        }
    }

    private fun listenToSchedule() {
        scheduleJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false, today = operationalDateOf(clock.now(), dayStartMinutes = 0)) }
        scheduleJob = viewModelScope.launch {
            observeMySchedule()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { shifts -> _state.update { it.copy(isLoading = false, days = shifts.byStartDay()) } }
        }
    }

    // Without the open entry, marking still works: the use case reads it again before writing.
    private fun listenToOpenEntry() {
        viewModelScope.launch {
            observeOpenEntry()
                .catch { updateMarking { it.copy(isLoading = false) } }
                .collect { entry -> updateMarking { it.copy(isLoading = false, openEntry = entry) } }
        }
    }

    private fun askToMark() {
        val marking = _state.value.marking ?: return
        if (marking.isMarking) return
        when (locationAccess()) {
            LocationAccess.GRANTED -> mark()
            LocationAccess.NEEDS_PERMISSION -> _state.update { it.copy(isExplainingPermission = true) }
            LocationAccess.UNAVAILABLE -> fail(AttendanceError.LocationUnavailable)
        }
    }

    private fun mark() {
        val marking = _state.value.marking ?: return
        if (marking.isMarking) return
        val kind = marking.nextMark
        updateMarking { it.copy(isMarking = true, result = null) }
        viewModelScope.launch {
            val result = markWithPhone(kind).fold(
                onSuccess = { MarkResult.Done(it) },
                onFailure = { MarkResult.Failed(kind, it as? AttendanceError ?: AttendanceError.Unknown) },
            )
            updateMarking { it.copy(isMarking = false, result = result) }
        }
    }

    private fun fail(error: AttendanceError) {
        updateMarking { it.copy(result = MarkResult.Failed(it.nextMark, error)) }
    }

    private fun updateMarking(change: (PhoneMarking) -> PhoneMarking) {
        _state.update { state -> state.marking?.let { state.copy(marking = change(it)) } ?: state }
    }
}
