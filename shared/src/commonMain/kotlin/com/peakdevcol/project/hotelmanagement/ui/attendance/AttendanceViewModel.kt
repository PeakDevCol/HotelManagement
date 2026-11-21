package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftsOfDay
import com.peakdevcol.project.hotelmanagement.domain.model.byStartDay
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveMyScheduleUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class AttendanceState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** RF-ASI-02: the shifts of the next two weeks, by day. */
    val days: List<ShiftsOfDay> = emptyList(),
    /** To say "Hoy" and "Mañana" instead of the date. */
    val today: CalendarDate? = null,
)

sealed interface AttendanceIntent {
    data object Retry : AttendanceIntent
}

/** RF-ASI-02. The second PR of M8 adds marking the entry and the exit here. */
class AttendanceViewModel(
    private val observeMySchedule: ObserveMyScheduleUseCase,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()

    private var scheduleJob: Job? = null

    init {
        listenToSchedule()
    }

    fun onIntent(intent: AttendanceIntent) {
        when (intent) {
            AttendanceIntent.Retry -> listenToSchedule()
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
}
