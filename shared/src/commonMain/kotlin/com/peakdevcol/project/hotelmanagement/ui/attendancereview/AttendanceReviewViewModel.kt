package com.peakdevcol.project.hotelmanagement.ui.attendancereview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceData
import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeHours
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkTimeChoice
import com.peakdevcol.project.hotelmanagement.domain.model.PeriodPreset
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.entryNear
import com.peakdevcol.project.hotelmanagement.domain.model.exitAfter
import com.peakdevcol.project.hotelmanagement.domain.model.formatClockTime
import com.peakdevcol.project.hotelmanagement.domain.model.minuteOfDayIn
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.model.parseClockTime
import com.peakdevcol.project.hotelmanagement.domain.model.pendingExits
import com.peakdevcol.project.hotelmanagement.domain.model.report
import com.peakdevcol.project.hotelmanagement.domain.model.shiftAmong
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_REASON_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.usecase.CorrectMarkUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadAttendanceReportUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntriesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RegisterMissingMarksUseCase
import com.peakdevcol.project.hotelmanagement.ui.currentorders.everyMinute
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
import kotlin.time.Clock
import kotlin.time.Instant

enum class ReviewTab {
    TO_REVIEW,
    HOURS,
}

/** What the administrator is typing in the sheet of a record. */
sealed interface EditorForm {
    val reasonText: String

    /** Another time for a mark, or the exit that was missing (RF-ASI-06). */
    data class MarkTime(val mark: MarkKind, val timeText: String, override val reasonText: String = "") : EditorForm

    /** Decision 4A: the entry and the exit of a shift that has no marks. */
    data class Registration(val entryText: String, val exitText: String, override val reasonText: String = "") : EditorForm
}

/** RF-ASI-07: the sheet of one record, with its marks and its history, or the registration of a shift. */
data class RecordEditor(
    val employeeId: String,
    /** Null while registering the marks of a shift that has none. */
    val record: AttendanceRecord?,
    /** The shift the record counts for, to show it and to suggest its times. */
    val shift: Shift?,
    /** Null shows the record; otherwise, what is being typed. */
    val form: EditorForm? = null,
    val error: CorrectionError? = null,
) {
    /** The moment the time typed for a mark means, to show it before saving. */
    val typedTime: Instant?
        get() {
            val form = form as? EditorForm.MarkTime ?: return null
            val minutes = parseClockTime(form.timeText) ?: return null
            val entry = record?.entryTime ?: return null
            return when (form.mark) {
                MarkKind.ENTRY -> entryNear(entry, minutes)
                MarkKind.EXIT -> exitAfter(entry, minutes)
            }
        }

    /** The entry and the exit typed for a shift without marks: the exit not after the entry is the next day. */
    val typedRegistration: Pair<Instant, Instant>?
        get() {
            val form = form as? EditorForm.Registration ?: return null
            val shift = shift ?: return null
            val entry = entryNear(shift.startAt, parseClockTime(form.entryText) ?: return null)
            return entry to exitAfter(entry, parseClockTime(form.exitText) ?: return null)
        }
}

data class AttendanceReviewState(
    val today: CalendarDate,
    val now: Instant,
    /** Null when the days were chosen one by one. */
    val preset: PeriodPreset? = PeriodPreset.THIS_FORTNIGHT,
    val period: AttendancePeriod,
    val tab: ReviewTab = ReviewTab.TO_REVIEW,
    val isLoading: Boolean = true,
    val loadError: ReportError? = null,
    val data: AttendanceData? = null,
    /** When [data] was read, so the administrator knows how fresh it is. */
    val loadedAt: Instant? = null,
    val report: AttendanceReport? = null,
    /** Every entry without exit, live, so a forgotten one comes up without reading the period again. */
    val openEntries: List<AttendanceRecord> = emptyList(),
    /** RF-ASI-06: the forgotten ones among [openEntries], from any day. */
    val pendingExits: List<AttendanceRecord> = emptyList(),
    val employees: List<User> = emptyList(),
    val isPickingDays: Boolean = false,
    val editor: RecordEditor? = null,
    val isSaving: Boolean = false,
) {
    val toReviewCount: Int get() = pendingExits.size + (report?.toConfirm?.size ?: 0)

    fun nameOf(employeeId: String): String? = employees.firstOrNull { it.id == employeeId }?.fullName

    fun hoursOf(employeeId: String): EmployeeHours? = report?.employees?.firstOrNull { it.employeeId == employeeId }

    /** The employees of the report, by name. */
    val employeeHours: List<EmployeeHours>
        get() = report?.employees.orEmpty().sortedBy { nameOf(it.employeeId)?.lowercase() ?: it.employeeId }
}

sealed interface AttendanceReviewIntent {
    data class SelectPreset(val preset: PeriodPreset) : AttendanceReviewIntent
    data object PickDays : AttendanceReviewIntent
    data class DaysPicked(val firstEpochDay: Long, val lastEpochDay: Long) : AttendanceReviewIntent
    data object DismissDays : AttendanceReviewIntent
    data class SelectTab(val tab: ReviewTab) : AttendanceReviewIntent
    data object Reload : AttendanceReviewIntent
    data class OpenRecord(val recordId: String) : AttendanceReviewIntent

    /** RF-ASI-06: straight to writing the exit of a forgotten entry. */
    data class SetMissingExit(val recordId: String) : AttendanceReviewIntent

    /** Decision 4A. */
    data class RegisterShift(val shiftId: String) : AttendanceReviewIntent
    data class UseDeviceTime(val mark: MarkKind) : AttendanceReviewIntent
    data class UseServerTime(val mark: MarkKind) : AttendanceReviewIntent
    data class EditTime(val mark: MarkKind) : AttendanceReviewIntent
    data class TimeChange(val text: String) : AttendanceReviewIntent
    data class EntryChange(val text: String) : AttendanceReviewIntent
    data class ExitChange(val text: String) : AttendanceReviewIntent
    data class ReasonChange(val text: String) : AttendanceReviewIntent
    data object Save : AttendanceReviewIntent

    /** From what is being typed back to the record, or closing the sheet when there is no record. */
    data object CancelForm : AttendanceReviewIntent
    data object DismissEditor : AttendanceReviewIntent
}

sealed interface AttendanceReviewEffect {
    data object Corrected : AttendanceReviewEffect
    data object Registered : AttendanceReviewEffect
    data class PeriodRejected(val error: ReportError) : AttendanceReviewEffect
}

/**
 * RF-ASI-05 to RF-ASI-08: what waits for the administrator and the hours of the team in a period.
 * The period is read once from the server; a correction changes the record here instead of
 * reading the period again.
 */
class AttendanceReviewViewModel(
    private val loadAttendanceReport: LoadAttendanceReportUseCase,
    observeOpenEntries: ObserveOpenEntriesUseCase,
    observeEmployees: ObserveEmployeesUseCase,
    private val correctMark: CorrectMarkUseCase,
    private val registerMissingMarks: RegisterMissingMarksUseCase,
    private val clock: Clock = Clock.System,
    minuteTicks: Flow<Instant> = everyMinute(clock),
) : ViewModel() {

    private val _state = MutableStateFlow(initialState(clock.now()))
    val state: StateFlow<AttendanceReviewState> = _state.asStateFlow()

    private val _effects = Channel<AttendanceReviewEffect>(Channel.BUFFERED)
    val effects: Flow<AttendanceReviewEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            observeEmployees().catch { emit(emptyList()) }.collect { employees -> _state.update { it.copy(employees = employees) } }
        }
        viewModelScope.launch {
            observeOpenEntries().catch { emit(emptyList()) }.collect { entries -> updateDerived { it.copy(openEntries = entries) } }
        }
        // A forgotten entry becomes a pending exit while the screen is open (RF-ASI-06).
        viewModelScope.launch { minuteTicks.collect { now -> updateDerived { it.copy(now = now) } } }
    }

    fun onIntent(intent: AttendanceReviewIntent) {
        when (intent) {
            is AttendanceReviewIntent.SelectPreset -> showPeriod(intent.preset.periodFor(_state.value.today), intent.preset)
            AttendanceReviewIntent.PickDays -> _state.update { it.copy(isPickingDays = true) }
            is AttendanceReviewIntent.DaysPicked -> pickDays(intent.firstEpochDay, intent.lastEpochDay)
            AttendanceReviewIntent.DismissDays -> _state.update { it.copy(isPickingDays = false) }
            is AttendanceReviewIntent.SelectTab -> _state.update { it.copy(tab = intent.tab) }
            AttendanceReviewIntent.Reload -> load()
            is AttendanceReviewIntent.OpenRecord -> openRecord(intent.recordId)
            is AttendanceReviewIntent.SetMissingExit -> openRecord(intent.recordId) { _, shift -> exitForm(shift) }
            is AttendanceReviewIntent.RegisterShift -> openRegistration(intent.shiftId)
            is AttendanceReviewIntent.UseDeviceTime -> correct(intent.mark, MarkTimeChoice.DeviceTime)
            is AttendanceReviewIntent.UseServerTime -> correct(intent.mark, MarkTimeChoice.ServerTime)
            is AttendanceReviewIntent.EditTime -> editTime(intent.mark)
            is AttendanceReviewIntent.TimeChange -> editForm { (it as? EditorForm.MarkTime)?.copy(timeText = intent.text.clockCharacters()) ?: it }
            is AttendanceReviewIntent.EntryChange -> editForm { (it as? EditorForm.Registration)?.copy(entryText = intent.text.clockCharacters()) ?: it }
            is AttendanceReviewIntent.ExitChange -> editForm { (it as? EditorForm.Registration)?.copy(exitText = intent.text.clockCharacters()) ?: it }
            is AttendanceReviewIntent.ReasonChange -> editForm { form -> form.withReason(intent.text.take(MAX_REASON_LENGTH)) }
            AttendanceReviewIntent.Save -> save()
            AttendanceReviewIntent.CancelForm -> cancelForm()
            AttendanceReviewIntent.DismissEditor -> _state.update { if (it.isSaving) it else it.copy(editor = null) }
        }
    }

    private fun initialState(now: Instant): AttendanceReviewState {
        val today = operationalDateOf(now, dayStartMinutes = 0)
        return AttendanceReviewState(today = today, now = now, period = PeriodPreset.THIS_FORTNIGHT.periodFor(today))
    }

    private fun showPeriod(period: AttendancePeriod, preset: PeriodPreset?) {
        _state.update { it.copy(period = period, preset = preset, isPickingDays = false) }
        load()
    }

    private fun pickDays(firstEpochDay: Long, lastEpochDay: Long) {
        _state.update { it.copy(isPickingDays = false) }
        AttendancePeriod.between(CalendarDate.fromEpochDay(firstEpochDay), CalendarDate.fromEpochDay(lastEpochDay)).fold(
            onSuccess = { period -> showPeriod(period, preset = null) },
            onFailure = { error -> _effects.trySend(AttendanceReviewEffect.PeriodRejected(error as? ReportError ?: ReportError.Unknown)) },
        )
    }

    // A reading that fails keeps the report of before on screen, with the error over it.
    private fun load() {
        loadJob?.cancel()
        val period = _state.value.period
        _state.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            loadAttendanceReport(period).fold(
                onSuccess = { data -> updateDerived { it.copy(isLoading = false, data = data, loadedAt = clock.now()) } },
                onFailure = { error -> _state.update { it.copy(isLoading = false, loadError = error as? ReportError ?: ReportError.Unknown) } },
            )
        }
    }

    private fun findRecord(recordId: String): AttendanceRecord? {
        val state = _state.value
        return state.data?.records?.firstOrNull { it.id == recordId } ?: state.openEntries.firstOrNull { it.id == recordId }
    }

    private fun shiftOf(record: AttendanceRecord): Shift? = _state.value.data?.let { record.shiftAmong(it.shifts) }

    /** Without [form], the sheet shows the record. */
    private fun openRecord(recordId: String, form: (AttendanceRecord, Shift?) -> EditorForm? = { _, _ -> null }) {
        val record = findRecord(recordId) ?: return
        val shift = shiftOf(record)
        _state.update { it.copy(editor = RecordEditor(record.employeeId, record, shift, form = form(record, shift))) }
    }

    private fun openRegistration(shiftId: String) {
        val shift = _state.value.data?.shifts?.firstOrNull { it.id == shiftId } ?: return
        val form = EditorForm.Registration(entryText = shift.startAt.clockText(), exitText = shift.endAt.clockText())
        _state.update { it.copy(editor = RecordEditor(shift.employeeId, record = null, shift = shift, form = form)) }
    }

    /** The exit that was missing starts at the end of the shift, when the entry counts for one. */
    private fun exitForm(shift: Shift?) =
        EditorForm.MarkTime(MarkKind.EXIT, timeText = shift?.endAt?.clockText().orEmpty())

    private fun editTime(mark: MarkKind) {
        val record = _state.value.editor?.record ?: return
        val current = if (mark == MarkKind.ENTRY) record.entryTime else record.exitTime
        val form = if (current == null) exitForm(shiftOf(record)) else EditorForm.MarkTime(mark, current.clockText())
        _state.update { state -> state.copy(editor = state.editor?.copy(form = form, error = null)) }
    }

    private fun editForm(change: (EditorForm) -> EditorForm) = _state.update { state ->
        val editor = state.editor ?: return@update state
        val form = editor.form ?: return@update state
        state.copy(editor = editor.copy(form = change(form), error = null))
    }

    private fun cancelForm() = _state.update { state ->
        val editor = state.editor ?: return@update state
        if (state.isSaving) state else state.copy(editor = if (editor.record == null) null else editor.copy(form = null, error = null))
    }

    private fun save() {
        val editor = _state.value.editor ?: return
        when (val form = editor.form) {
            is EditorForm.MarkTime -> {
                val time = editor.typedTime ?: return showError(CorrectionError.InvalidTime)
                correct(form.mark, MarkTimeChoice.OtherTime(time, form.reasonText))
            }
            is EditorForm.Registration -> register(editor, form)
            null -> Unit
        }
    }

    private fun correct(mark: MarkKind, choice: MarkTimeChoice) {
        val record = _state.value.editor?.record ?: return
        work(AttendanceReviewEffect.Corrected) { correctMark(record, mark, choice) }
    }

    private fun register(editor: RecordEditor, form: EditorForm.Registration) {
        val (entry, exit) = editor.typedRegistration ?: return showError(CorrectionError.InvalidTime)
        work(AttendanceReviewEffect.Registered) { registerMissingMarks(editor.employeeId, entry, exit, form.reasonText) }
    }

    // Every error stays in the sheet: a message below it would be hidden by the sheet itself.
    private fun work(done: AttendanceReviewEffect, action: suspend () -> Result<AttendanceRecord>) {
        if (_state.value.isSaving) return
        _state.update { state -> state.copy(isSaving = true, editor = state.editor?.copy(error = null)) }
        viewModelScope.launch {
            action().fold(
                onSuccess = { record ->
                    updateDerived { it.copy(isSaving = false, editor = null, data = it.data?.withRecord(record)) }
                    _effects.send(done)
                },
                onFailure = { error ->
                    _state.update { state -> state.copy(isSaving = false, editor = state.editor?.copy(error = error as? CorrectionError ?: CorrectionError.Unknown)) }
                },
            )
        }
    }

    private fun showError(error: CorrectionError) = _state.update { state -> state.copy(editor = state.editor?.copy(error = error)) }

    /** The report and the pending exits follow the data, the open entries and the time. */
    private fun updateDerived(change: (AttendanceReviewState) -> AttendanceReviewState) = _state.update { state ->
        val changed = change(state)
        val data = changed.data ?: return@update changed
        changed.copy(report = data.report(changed.now), pendingExits = changed.openEntries.pendingExits(changed.now, data.maxOpenEntryHours))
    }

    private fun EditorForm.withReason(text: String): EditorForm = when (this) {
        is EditorForm.MarkTime -> copy(reasonText = text)
        is EditorForm.Registration -> copy(reasonText = text)
    }

    private fun Instant.clockText(): String = formatClockTime(minuteOfDayIn(this))

    private fun String.clockCharacters() = filter { it.isDigit() || it == ':' }.take(MAX_TIME_LENGTH)

    private companion object {
        const val MAX_TIME_LENGTH = 5
    }
}
