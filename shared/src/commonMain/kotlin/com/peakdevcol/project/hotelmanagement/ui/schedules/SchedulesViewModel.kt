package com.peakdevcol.project.hotelmanagement.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftDraft
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.formatClockTime
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.model.parseClockTime
import com.peakdevcol.project.hotelmanagement.domain.model.weekStartOf
import com.peakdevcol.project.hotelmanagement.domain.usecase.CopyPreviousWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteShiftUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveScheduleWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveShiftUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ScheduleDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** A shift as it is being written. A null [shiftId] is a new shift. */
data class ShiftForm(
    val shiftId: String? = null,
    val employeeId: String? = null,
    val day: CalendarDate,
    /** The times as they are typed, on a 24-hour clock. */
    val startText: String = "",
    val endText: String = "",
    val area: UserArea? = null,
    val error: ScheduleError? = null,
) {
    fun toDraft() = ShiftDraft(employeeId, day, parseClockTime(startText), parseClockTime(endText), area)
}

data class SchedulesState(
    /** The Monday of the week shown. */
    val monday: CalendarDate,
    val thisMonday: CalendarDate,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val days: List<ScheduleDay> = emptyList(),
    /** Every employee, for the names; only the active ones get new shifts. */
    val employees: List<User> = emptyList(),
    val form: ShiftForm? = null,
    val isConfirmingCopy: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val isWorking: Boolean = false,
) {
    val activeEmployees: List<User> get() = employees.filter { it.active }
    val overlapCount: Int get() = days.sumOf { day -> day.shifts.count { it.overlapsAnother } }
    val isEmpty: Boolean get() = days.all { it.shifts.isEmpty() }

    fun nameOf(employeeId: String): String? = employees.firstOrNull { it.id == employeeId }?.fullName
}

sealed interface SchedulesIntent {
    data object PreviousWeek : SchedulesIntent
    data object NextWeek : SchedulesIntent
    data object ThisWeek : SchedulesIntent
    data object Retry : SchedulesIntent

    /** A new shift, on [day] when it comes from that day of the week. */
    data class NewShift(val day: CalendarDate? = null) : SchedulesIntent
    data class EditShift(val shift: Shift) : SchedulesIntent
    data class SelectEmployee(val employeeId: String) : SchedulesIntent
    data class SelectDay(val day: CalendarDate) : SchedulesIntent
    data class StartChange(val text: String) : SchedulesIntent
    data class EndChange(val text: String) : SchedulesIntent

    /** A null area is the usual area of the employee. */
    data class SelectArea(val area: UserArea?) : SchedulesIntent
    data object SaveShift : SchedulesIntent
    data object DeleteShift : SchedulesIntent
    data object ConfirmDelete : SchedulesIntent
    data object CopyPreviousWeek : SchedulesIntent
    data object ConfirmCopy : SchedulesIntent
    data object Dismiss : SchedulesIntent
}

sealed interface SchedulesEffect {
    data object Saved : SchedulesEffect
    data object Deleted : SchedulesEffect
    data class Copied(val count: Int) : SchedulesEffect
    data class Failed(val error: ScheduleError) : SchedulesEffect
}

/** RF-ASI-01: an administrator plans the shifts of the team, week by week. */
@OptIn(ExperimentalCoroutinesApi::class)
class SchedulesViewModel(
    observeScheduleWeek: ObserveScheduleWeekUseCase,
    observeEmployees: ObserveEmployeesUseCase,
    private val saveShift: SaveShiftUseCase,
    private val deleteShift: DeleteShiftUseCase,
    private val copyPreviousWeek: CopyPreviousWeekUseCase,
    clock: Clock = Clock.System,
) : ViewModel() {

    private val thisMonday = weekStartOf(operationalDateOf(clock.now(), dayStartMinutes = 0))
    private val _state = MutableStateFlow(SchedulesState(monday = thisMonday, thisMonday = thisMonday))
    val state: StateFlow<SchedulesState> = _state.asStateFlow()

    private val _effects = Channel<SchedulesEffect>(Channel.BUFFERED)
    val effects: Flow<SchedulesEffect> = _effects.receiveAsFlow()

    // Bumped by "Reintentar", so the week is read again.
    private val attempts = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            observeEmployees()
                .catch { emit(emptyList()) }
                .collect { employees -> _state.update { it.copy(employees = employees.sortedBy { employee -> employee.fullName.lowercase() }) } }
        }
        viewModelScope.launch {
            _state.map { it.monday }.distinctUntilChanged()
                .flatMapLatest { monday ->
                    attempts.flatMapLatest {
                        observeScheduleWeek(monday)
                            .onEach { days -> _state.update { it.copy(isLoading = false, loadFailed = false, days = days) } }
                            .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                    }
                }
                .collect()
        }
    }

    fun onIntent(intent: SchedulesIntent) {
        when (intent) {
            SchedulesIntent.PreviousWeek -> showWeek(_state.value.monday.plusDays(-7))
            SchedulesIntent.NextWeek -> showWeek(_state.value.monday.plusDays(7))
            SchedulesIntent.ThisWeek -> showWeek(thisMonday)
            SchedulesIntent.Retry -> {
                _state.update { it.copy(isLoading = true, loadFailed = false) }
                attempts.update { it + 1 }
            }
            is SchedulesIntent.NewShift -> _state.update { it.copy(form = ShiftForm(day = intent.day ?: it.monday)) }
            is SchedulesIntent.EditShift -> _state.update { it.copy(form = intent.shift.toForm()) }
            is SchedulesIntent.SelectEmployee -> editForm { it.copy(employeeId = intent.employeeId) }
            is SchedulesIntent.SelectDay -> editForm { it.copy(day = intent.day) }
            is SchedulesIntent.StartChange -> editForm { it.copy(startText = intent.text.clockCharacters()) }
            is SchedulesIntent.EndChange -> editForm { it.copy(endText = intent.text.clockCharacters()) }
            is SchedulesIntent.SelectArea -> editForm { it.copy(area = intent.area) }
            SchedulesIntent.SaveShift -> save()
            SchedulesIntent.DeleteShift -> _state.update { if (it.form?.shiftId != null) it.copy(isConfirmingDelete = true) else it }
            SchedulesIntent.ConfirmDelete -> delete()
            SchedulesIntent.CopyPreviousWeek -> _state.update { it.copy(isConfirmingCopy = true) }
            SchedulesIntent.ConfirmCopy -> copy()
            SchedulesIntent.Dismiss -> _state.update { it.copy(form = null, isConfirmingCopy = false, isConfirmingDelete = false) }
        }
    }

    private fun showWeek(monday: CalendarDate) = _state.update { it.copy(monday = monday, isLoading = true, loadFailed = false) }

    private fun Shift.toForm(): ShiftForm {
        val draft = ShiftDraft.of(this)
        return ShiftForm(
            shiftId = id,
            employeeId = employeeId,
            day = draft.day,
            startText = draft.startMinutes?.let(::formatClockTime).orEmpty(),
            endText = draft.endMinutes?.let(::formatClockTime).orEmpty(),
            area = area,
        )
    }

    private fun String.clockCharacters() = filter { it.isDigit() || it == ':' }.take(MAX_TIME_LENGTH)

    private fun editForm(change: (ShiftForm) -> ShiftForm) = _state.update { state ->
        state.form?.let { state.copy(form = change(it).copy(error = null)) } ?: state
    }

    // A wrong field keeps the form open with its error; anything else closes it and says so.
    private fun save() {
        val form = _state.value.form ?: return
        work {
            val error = saveShift(form.shiftId, form.toDraft()).exceptionOrNull()?.toScheduleError()
            if (error in FORM_ERRORS) {
                _state.update { it.copy(form = it.form?.copy(error = error)) }
                null
            } else {
                _state.update { it.copy(form = null) }
                error?.let { SchedulesEffect.Failed(it) } ?: SchedulesEffect.Saved
            }
        }
    }

    private fun delete() {
        val shiftId = _state.value.form?.shiftId ?: return
        _state.update { it.copy(isConfirmingDelete = false, form = null) }
        work { deleteShift(shiftId).fold({ SchedulesEffect.Deleted }, { SchedulesEffect.Failed(it.toScheduleError()) }) }
    }

    private fun copy() {
        val monday = _state.value.monday
        _state.update { it.copy(isConfirmingCopy = false) }
        work { copyPreviousWeek(monday).fold({ SchedulesEffect.Copied(it) }, { SchedulesEffect.Failed(it.toScheduleError()) }) }
    }

    private fun work(action: suspend () -> SchedulesEffect?) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val effect = action()
            _state.update { it.copy(isWorking = false) }
            effect?.let { _effects.send(it) }
        }
    }

    private fun Throwable.toScheduleError(): ScheduleError = this as? ScheduleError ?: ScheduleError.Unknown

    private companion object {
        const val MAX_TIME_LENGTH = 5
        val FORM_ERRORS = setOf(ScheduleError.MissingEmployee, ScheduleError.InvalidTime, ScheduleError.EmptyShift)
    }
}
