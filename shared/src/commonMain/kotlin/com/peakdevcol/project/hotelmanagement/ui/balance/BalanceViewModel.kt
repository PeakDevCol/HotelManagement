package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceFilter
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceOption
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceSummary
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.PendingCollection
import com.peakdevcol.project.hotelmanagement.domain.usecase.LoadBalanceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObservePendingCollectionUseCase
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

/** RF-BAL-03: the breakdown shown under the totals. */
enum class BalanceBreakdown {
    BY_DAY,
    BY_METHOD,
    BY_EMPLOYEE,
    BY_PRODUCT,
}

data class BalanceState(
    val isLoading: Boolean = true,
    /** Why the payments of the period could not be read. The screen offers to try again. */
    val loadError: PaymentReadError? = null,
    val report: BalanceReport? = null,
    val filter: BalanceFilter = BalanceFilter(),
    val summary: BalanceSummary? = null,
    val employeeOptions: List<BalanceOption> = emptyList(),
    val productOptions: List<BalanceOption> = emptyList(),
    val pending: PendingCollection? = null,
    /** RF-BAL-06: the days of the period that had payments and ended without a "Cierre del día". */
    val daysWithoutDailyClosing: Set<CalendarDate> = emptySet(),
    val breakdown: BalanceBreakdown = BalanceBreakdown.BY_DAY,
    val isPickingPeriod: Boolean = false,
    val isChoosingFilters: Boolean = false,
) {
    val period: BalancePeriod? get() = report?.period

    /** How many of the four filters are in use. */
    val activeFilterCount: Int
        get() = listOfNotNull(filter.method, filter.employeeId, filter.destination, filter.productId).size
}

sealed interface BalanceIntent {
    data object Reload : BalanceIntent
    data object OpenPeriodPicker : BalanceIntent
    data object DismissPeriodPicker : BalanceIntent
    data object OpenFilters : BalanceIntent
    data object DismissFilters : BalanceIntent
    data object ClearFilters : BalanceIntent

    /** The days come from the date picker, as days since 1970-01-01. */
    data class ChangePeriod(val firstEpochDay: Long, val lastEpochDay: Long) : BalanceIntent
    data class FilterByMethod(val method: PaymentMethod?) : BalanceIntent
    data class FilterByEmployee(val employeeId: String?) : BalanceIntent
    data class FilterByDestination(val destination: DestinationKind?) : BalanceIntent
    data class FilterByProduct(val productId: String?) : BalanceIntent
    data class ShowBreakdown(val breakdown: BalanceBreakdown) : BalanceIntent
}

sealed interface BalanceEffect {
    data class PeriodRejected(val error: BalanceError) : BalanceEffect
}

class BalanceViewModel(
    private val loadBalance: LoadBalanceUseCase,
    observePendingCollection: ObservePendingCollectionUseCase,
    observeEmployees: ObserveEmployeesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BalanceState())
    val state: StateFlow<BalanceState> = _state.asStateFlow()

    private val _effects = Channel<BalanceEffect>(Channel.BUFFERED)
    val effects: Flow<BalanceEffect> = _effects.receiveAsFlow()

    private var employeeNames: Map<String, String> = emptyMap()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeEmployees()
                .catch { emit(emptyList()) }
                .collect { employees ->
                    employeeNames = employees.associate { it.id to it.fullName }
                    _state.update { it.summarized() }
                }
        }
        viewModelScope.launch {
            observePendingCollection()
                .catch { }
                .collect { pending -> _state.update { it.copy(pending = pending) } }
        }
        load(period = null)
    }

    fun onIntent(intent: BalanceIntent) {
        when (intent) {
            BalanceIntent.Reload -> load(_state.value.period)
            BalanceIntent.OpenPeriodPicker -> _state.update { it.copy(isPickingPeriod = true) }
            BalanceIntent.DismissPeriodPicker -> _state.update { it.copy(isPickingPeriod = false) }
            BalanceIntent.OpenFilters -> _state.update { it.copy(isChoosingFilters = true) }
            BalanceIntent.DismissFilters -> _state.update { it.copy(isChoosingFilters = false) }
            BalanceIntent.ClearFilters -> filter { BalanceFilter() }
            is BalanceIntent.ChangePeriod -> changePeriod(intent)
            is BalanceIntent.FilterByMethod -> filter { it.copy(method = intent.method) }
            is BalanceIntent.FilterByEmployee -> filter { it.copy(employeeId = intent.employeeId) }
            is BalanceIntent.FilterByDestination -> filter { it.copy(destination = intent.destination) }
            is BalanceIntent.FilterByProduct -> filter { it.copy(productId = intent.productId) }
            is BalanceIntent.ShowBreakdown -> _state.update { it.copy(breakdown = intent.breakdown) }
        }
    }

    private fun changePeriod(intent: BalanceIntent.ChangePeriod) {
        load(BalancePeriod(CalendarDate.fromEpochDay(intent.firstEpochDay), CalendarDate.fromEpochDay(intent.lastEpochDay)))
    }

    private fun load(period: BalancePeriod?) {
        // Only the last period asked for counts: an older reading that arrives later is discarded.
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            val result = loadBalance(period)
            val report = result.getOrNull()
            when (val error = result.exceptionOrNull()) {
                null -> _state.update {
                    it.copy(isLoading = false, isPickingPeriod = false, report = report).keepingAvailableFilters().summarized()
                }
                // The period shown stays the one before: the screen explains why the new one was not used.
                is BalanceError -> {
                    _state.update { it.copy(isLoading = false, isPickingPeriod = false) }
                    _effects.send(BalanceEffect.PeriodRejected(error))
                }
                else -> _state.update { it.copy(isLoading = false, loadError = error as? PaymentReadError ?: PaymentReadError.Unknown) }
            }
        }
    }

    // RNF-12: filtering works on the payments already read, without reading them again.
    private fun filter(change: (BalanceFilter) -> BalanceFilter) {
        _state.update { it.copy(filter = change(it.filter)).summarized() }
    }

    /** A new period may not have the employee or the product that was chosen: that filter is dropped. */
    private fun BalanceState.keepingAvailableFilters(): BalanceState {
        val report = report ?: return this
        val employeeIds = report.employeeOptions(employeeNames).map { it.id }.toSet()
        val productIds = report.productOptions().map { it.id }.toSet()
        return copy(
            filter = filter.copy(
                employeeId = filter.employeeId?.takeIf { it in employeeIds },
                productId = filter.productId?.takeIf { it in productIds },
            ),
        )
    }

    private fun BalanceState.summarized(): BalanceState {
        val report = report ?: return this
        return copy(
            summary = report.summary(filter, employeeNames),
            daysWithoutDailyClosing = report.daysWithoutDailyClosing(),
            employeeOptions = report.employeeOptions(employeeNames),
            productOptions = report.productOptions(),
        )
    }
}
