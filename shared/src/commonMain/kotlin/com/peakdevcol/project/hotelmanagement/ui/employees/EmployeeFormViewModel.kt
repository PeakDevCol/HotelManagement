package com.peakdevcol.project.hotelmanagement.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Access
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeCreation
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.UpdateEmployeeUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployeeFormState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val notFound: Boolean = false,
    val fullName: String = "",
    val email: String = "",
    val type: UserType = UserType.BASIC,
    val area: UserArea = UserArea.SERVICE,
    val active: Boolean = true,
    /** RF-EMP-03: an administrator does not change their own role nor deactivate themselves. */
    val isOwnProfile: Boolean = false,
    /** The field the last attempt to save found wrong. */
    val fieldError: EmployeeError? = null,
    val isWorking: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !notFound && !isWorking
    val canChangeAccess: Boolean get() = !isOwnProfile
}

sealed interface EmployeeFormIntent {
    data class NameChange(val fullName: String) : EmployeeFormIntent
    data class EmailChange(val email: String) : EmployeeFormIntent
    data class TypeSelect(val type: UserType) : EmployeeFormIntent
    data class AreaSelect(val area: UserArea) : EmployeeFormIntent
    data class ActiveChange(val active: Boolean) : EmployeeFormIntent
    data object Save : EmployeeFormIntent
    data object SendPasswordEmail : EmployeeFormIntent
}

sealed interface EmployeeFormEffect {
    data class Created(val creation: EmployeeCreation) : EmployeeFormEffect
    data object Saved : EmployeeFormEffect
    data object PasswordEmailSent : EmployeeFormEffect
    data class Failed(val error: EmployeeError) : EmployeeFormEffect
}

class EmployeeFormViewModel(
    private val employeeId: String?,
    private val observeEmployees: ObserveEmployeesUseCase,
    private val observeAccess: ObserveAccessUseCase,
    private val createEmployee: CreateEmployeeUseCase,
    private val updateEmployee: UpdateEmployeeUseCase,
    private val sendPasswordEmail: SendPasswordEmailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeFormState(isNew = employeeId == null, isLoading = employeeId != null))
    val state: StateFlow<EmployeeFormState> = _state.asStateFlow()

    private val _effects = Channel<EmployeeFormEffect>(Channel.BUFFERED)
    val effects: Flow<EmployeeFormEffect> = _effects.receiveAsFlow()

    init {
        if (employeeId != null) loadEmployee(employeeId)
    }

    fun onIntent(intent: EmployeeFormIntent) {
        when (intent) {
            is EmployeeFormIntent.NameChange -> edit { it.copy(fullName = intent.fullName.take(MAX_NAME_LENGTH)) }
            is EmployeeFormIntent.EmailChange -> edit { if (it.isNew) it.copy(email = intent.email.trim()) else it }
            is EmployeeFormIntent.TypeSelect -> edit { if (it.canChangeAccess) it.copy(type = intent.type) else it }
            is EmployeeFormIntent.AreaSelect -> edit { it.copy(area = intent.area) }
            is EmployeeFormIntent.ActiveChange -> edit { if (it.canChangeAccess) it.copy(active = intent.active) else it }
            EmployeeFormIntent.Save -> save()
            EmployeeFormIntent.SendPasswordEmail -> sendEmail()
        }
    }

    // The form is filled once: later changes from the server must not overwrite what is being typed.
    private fun loadEmployee(id: String) {
        viewModelScope.launch {
            val employee = runCatching { observeEmployees().first().firstOrNull { it.id == id } }.getOrNull()
            val currentUserId = runCatching { observeAccess().first { it is Access.Granted } as Access.Granted }.getOrNull()?.user?.id
            _state.update { state ->
                if (employee == null) {
                    state.copy(isLoading = false, notFound = true)
                } else {
                    state.copy(
                        isLoading = false,
                        fullName = employee.fullName,
                        email = employee.email,
                        type = employee.type,
                        area = employee.area,
                        active = employee.active,
                        isOwnProfile = employee.id == currentUserId,
                    )
                }
            }
        }
    }

    private fun edit(change: (EmployeeFormState) -> EmployeeFormState) =
        _state.update { change(it).copy(fieldError = null) }

    private fun save() {
        val form = _state.value
        if (!form.canSave) return
        work {
            if (employeeId == null) {
                createEmployee(form.fullName, form.email, form.type, form.area).map { EmployeeFormEffect.Created(it) }
            } else {
                val employee = User(employeeId, form.fullName, form.email, form.type, form.area, form.active)
                updateEmployee(employee).map { EmployeeFormEffect.Saved }
            }
        }
    }

    private fun sendEmail() {
        val form = _state.value
        if (form.isNew || !form.canSave) return
        work { sendPasswordEmail(form.email).map { EmployeeFormEffect.PasswordEmailSent } }
    }

    // A wrong field is marked in the form; anything else is told in a message.
    private fun work(action: suspend () -> Result<EmployeeFormEffect>) {
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val result = action()
            val error = result.exceptionOrNull()?.let { it as? EmployeeError ?: EmployeeError.Unknown }
            _state.update { it.copy(isWorking = false, fieldError = error.takeIf { it in FIELD_ERRORS }) }
            when (error) {
                null -> _effects.send(result.getOrThrow())
                in FIELD_ERRORS -> Unit
                else -> _effects.send(EmployeeFormEffect.Failed(error))
            }
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
        val FIELD_ERRORS = setOf(EmployeeError.InvalidName, EmployeeError.InvalidEmail, EmployeeError.EmailAlreadyInUse)
    }
}
