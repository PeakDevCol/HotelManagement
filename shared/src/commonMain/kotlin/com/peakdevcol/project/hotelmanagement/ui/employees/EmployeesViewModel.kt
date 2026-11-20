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

/** An employee open beside the list, on wide windows. A null [employeeId] is a new employee. */
data class OpenEmployee(val employeeId: String?)

data class EmployeesState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val employees: List<User> = emptyList(),
    val openEmployee: OpenEmployee? = null,
)

sealed interface EmployeesIntent {
    /** [besideList] when the window shows the employee next to the list instead of on its own screen. */
    data class NewEmployee(val besideList: Boolean) : EmployeesIntent
    data class SelectEmployee(val employeeId: String, val besideList: Boolean) : EmployeesIntent
    data object CloseEmployee : EmployeesIntent
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
            is EmployeesIntent.NewEmployee -> open(employeeId = null, besideList = intent.besideList)
            is EmployeesIntent.SelectEmployee -> open(intent.employeeId, intent.besideList)
            EmployeesIntent.CloseEmployee -> _state.update { it.copy(openEmployee = null) }
            EmployeesIntent.Retry -> listenToEmployees()
        }
    }

    private fun open(employeeId: String?, besideList: Boolean) {
        if (besideList) {
            _state.update { it.copy(openEmployee = OpenEmployee(employeeId)) }
        } else {
            _effects.trySend(EmployeesEffect.NavigateToEmployee(employeeId))
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
