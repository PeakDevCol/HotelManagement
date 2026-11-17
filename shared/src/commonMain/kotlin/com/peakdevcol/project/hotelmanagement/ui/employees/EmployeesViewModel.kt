package com.peakdevcol.project.hotelmanagement.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
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

data class EmployeesState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val employees: List<User> = emptyList(),
)

sealed interface EmployeesIntent {
    data object NewEmployee : EmployeesIntent
    data class OpenEmployee(val employeeId: String) : EmployeesIntent
    data object Retry : EmployeesIntent
}

sealed interface EmployeesEffect {
    /** A null id opens the form to create an employee. */
    data class NavigateToEmployee(val employeeId: String?) : EmployeesEffect
}

class EmployeesViewModel(
    private val observeEmployees: ObserveEmployeesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EmployeesState())
    val state: StateFlow<EmployeesState> = _state.asStateFlow()

    private val _effects = Channel<EmployeesEffect>(Channel.BUFFERED)
    val effects: Flow<EmployeesEffect> = _effects.receiveAsFlow()

    private var employeesJob: Job? = null

    init {
        listenToEmployees()
    }

    fun onIntent(intent: EmployeesIntent) {
        when (intent) {
            EmployeesIntent.NewEmployee -> _effects.trySend(EmployeesEffect.NavigateToEmployee(employeeId = null))
            is EmployeesIntent.OpenEmployee -> _effects.trySend(EmployeesEffect.NavigateToEmployee(intent.employeeId))
            EmployeesIntent.Retry -> listenToEmployees()
        }
    }

    private fun listenToEmployees() {
        employeesJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        employeesJob = viewModelScope.launch {
            observeEmployees()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { employees -> _state.update { it.copy(isLoading = false, employees = employees) } }
        }
    }
}
