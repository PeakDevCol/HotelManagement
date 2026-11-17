package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeCreation
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.UpdateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAccountCreationRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormEffect
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormIntent
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormViewModel
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesEffect
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesIntent
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmployeeViewModelsTest : MainDispatcherTest() {
    private val admin = employee("admin", "Juan Felipe", type = UserType.ADMIN).copy(area = UserArea.MANAGEMENT)
    private val waiter = employee("waiter", "Ana Gómez")
    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(admin, waiter)
    private val accounts = FakeAccountCreationRepository()

    private fun list() = EmployeesViewModel(ObserveEmployeesUseCase(users))

    private fun form(employeeId: String?) = EmployeeFormViewModel(
        employeeId = employeeId,
        observeEmployees = ObserveEmployeesUseCase(users),
        observeAccess = ObserveAccessUseCase(auth, users),
        createEmployee = CreateEmployeeUseCase(auth, accounts, users),
        updateEmployee = UpdateEmployeeUseCase(auth, users),
        sendPasswordEmail = SendPasswordEmailUseCase(auth),
    )

    private fun stored(id: String) = list().state.value.employees.first { it.id == id }

    // ---- list

    @Test
    fun listsTheEmployeesAndFollowsTheirChanges() {
        val viewModel = list()
        assertEquals(listOf("Ana Gómez", "Juan Felipe"), viewModel.state.value.employees.map { it.fullName })

        users.save(waiter.copy(active = false))

        assertFalse(viewModel.state.value.employees.first().active)
    }

    @Test
    fun opensTheFormToCreateOrEditAnEmployee() = runTest {
        val viewModel = list()

        viewModel.onIntent(EmployeesIntent.NewEmployee)
        assertEquals(EmployeesEffect.NavigateToEmployee(employeeId = null), viewModel.effects.first())

        viewModel.onIntent(EmployeesIntent.OpenEmployee("waiter"))
        assertEquals(EmployeesEffect.NavigateToEmployee("waiter"), viewModel.effects.first())
    }

    @Test
    fun showsAnErrorAndRetries() {
        users.failure = IllegalStateException("permission denied")
        val viewModel = list()
        assertTrue(viewModel.state.value.loadFailed)

        users.failure = null
        viewModel.onIntent(EmployeesIntent.Retry)

        assertEquals(2, viewModel.state.value.employees.size)
    }

    // ---- form

    @Test
    fun aNewEmployeeStartsAsAnActiveEmployeeOfService() {
        val state = form(employeeId = null).state.value

        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertEquals(UserType.BASIC to UserArea.SERVICE, state.type to state.area)
    }

    @Test
    fun creatingAnEmployeeInvitesThemAndClosesTheForm() = runTest {
        val viewModel = form(employeeId = null)
        viewModel.onIntent(EmployeeFormIntent.NameChange("Luis Pérez"))
        viewModel.onIntent(EmployeeFormIntent.EmailChange(" luis@hotel.co "))
        viewModel.onIntent(EmployeeFormIntent.TypeSelect(UserType.RECEPTION))
        viewModel.onIntent(EmployeeFormIntent.AreaSelect(UserArea.RECEPTION))

        viewModel.onIntent(EmployeeFormIntent.Save)

        assertEquals(EmployeeFormEffect.Created(EmployeeCreation.INVITED), viewModel.effects.first())
        assertEquals(listOf("luis@hotel.co"), auth.passwordEmails)
        assertEquals(UserType.RECEPTION, stored(accounts.accounts.getValue("luis@hotel.co")).type)
    }

    @Test
    fun aWrongFieldIsMarkedAndNothingIsCreated() {
        val viewModel = form(employeeId = null)
        viewModel.onIntent(EmployeeFormIntent.NameChange("Luis Pérez"))
        viewModel.onIntent(EmployeeFormIntent.EmailChange("luis@"))

        viewModel.onIntent(EmployeeFormIntent.Save)
        assertEquals(EmployeeError.InvalidEmail, viewModel.state.value.fieldError)

        viewModel.onIntent(EmployeeFormIntent.EmailChange("luis@hotel.co"))
        assertNull(viewModel.state.value.fieldError)
        assertTrue(accounts.accounts.isEmpty())
    }

    @Test
    fun anEmailAlreadyRegisteredIsMarkedOnItsField() {
        accounts.accounts["luis@hotel.co"] = "existing"
        val viewModel = form(employeeId = null)
        viewModel.onIntent(EmployeeFormIntent.NameChange("Luis Pérez"))
        viewModel.onIntent(EmployeeFormIntent.EmailChange("luis@hotel.co"))

        viewModel.onIntent(EmployeeFormIntent.Save)

        assertEquals(EmployeeError.EmailAlreadyInUse, viewModel.state.value.fieldError)
        assertFalse(viewModel.state.value.isWorking)
    }

    @Test
    fun editingShowsTheEmployeeAndSavesTheChanges() = runTest {
        val viewModel = form(employeeId = "waiter")
        assertEquals("Ana Gómez" to "waiter@hotel.co", viewModel.state.value.let { it.fullName to it.email })
        assertFalse(viewModel.state.value.isOwnProfile)

        viewModel.onIntent(EmployeeFormIntent.AreaSelect(UserArea.HOUSEKEEPING))
        viewModel.onIntent(EmployeeFormIntent.ActiveChange(false))
        viewModel.onIntent(EmployeeFormIntent.Save)

        assertEquals(EmployeeFormEffect.Saved, viewModel.effects.first())
        assertEquals(waiter.copy(area = UserArea.HOUSEKEEPING, active = false), stored("waiter"))
    }

    @Test
    fun theEmailOfAnExistingEmployeeCannotBeEdited() {
        val viewModel = form(employeeId = "waiter")

        viewModel.onIntent(EmployeeFormIntent.EmailChange("other@hotel.co"))

        assertEquals("waiter@hotel.co", viewModel.state.value.email)
    }

    @Test
    fun anAdministratorCannotChangeTheirOwnRoleNorDeactivateThemselves() {
        val viewModel = form(employeeId = "admin")
        assertTrue(viewModel.state.value.isOwnProfile)

        viewModel.onIntent(EmployeeFormIntent.TypeSelect(UserType.BASIC))
        viewModel.onIntent(EmployeeFormIntent.ActiveChange(false))

        assertEquals(UserType.ADMIN to true, viewModel.state.value.let { it.type to it.active })
        assertFalse(viewModel.state.value.canChangeAccess)
    }

    @Test
    fun sendsThePasswordEmailAgainToAnEmployee() = runTest {
        val viewModel = form(employeeId = "waiter")

        viewModel.onIntent(EmployeeFormIntent.SendPasswordEmail)

        assertEquals(EmployeeFormEffect.PasswordEmailSent, viewModel.effects.first())
        assertEquals(listOf("waiter@hotel.co"), auth.passwordEmails)
    }

    @Test
    fun aRejectedChangeIsToldInAMessage() = runTest {
        users.writeFailure = EmployeeError.NotAllowed
        val viewModel = form(employeeId = "waiter")

        viewModel.onIntent(EmployeeFormIntent.Save)

        assertEquals(EmployeeFormEffect.Failed(EmployeeError.NotAllowed), viewModel.effects.first())
        assertNull(viewModel.state.value.fieldError)
    }

    @Test
    fun tellsWhenTheEmployeeToEditDoesNotExist() {
        assertTrue(form(employeeId = "gone").state.value.notFound)
    }
}
