package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveWhoIsWorkingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.WorkingEntry
import com.peakdevcol.project.hotelmanagement.ui.currentorders.everyMinute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class WhoIsWorkingState(
    /** RF-ASI-09: only reception and administrators see who is working. */
    val isVisible: Boolean = false,
    val loadFailed: Boolean = false,
    val entries: List<WorkingEntry> = emptyList(),
    val now: Instant? = null,
) {
    /** A forgotten entry is not someone working: it waits for the administrator (RF-ASI-06). */
    val working: List<WorkingEntry>
        get() = now?.let { now -> entries.filterNot { it.entry.isForgotten(now) } } ?: entries
}

/** RF-ASI-09: who has the entry marked, live. It only shows; nothing is changed from here. */
class WhoIsWorkingViewModel(
    observeWhoIsWorking: ObserveWhoIsWorkingUseCase,
    clock: Clock = Clock.System,
    minuteTicks: Flow<Instant> = everyMinute(clock),
) : ViewModel() {

    private val _state = MutableStateFlow(WhoIsWorkingState(now = clock.now()))
    val state: StateFlow<WhoIsWorkingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeWhoIsWorking()
                .catch { _state.update { it.copy(isVisible = true, loadFailed = true) } }
                .collect { entries -> _state.update { it.copy(isVisible = entries != null, loadFailed = false, entries = entries.orEmpty()) } }
        }
        // "Lleva 3 h 15 min" stays right, and an entry that becomes forgotten leaves the list.
        viewModelScope.launch { minuteTicks.collect { now -> _state.update { it.copy(now = now) } } }
    }
}
