package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeCreation
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.menuFor
import com.peakdevcol.project.hotelmanagement.domain.usecase.CreateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SendPasswordEmailUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.UpdateEmployeeUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAccountCreationRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmployeeUseCasesTest {
    private val admin = employee("admin", "Juan Felipe", type = UserType.ADMIN).copy(area = UserArea.MANAGEMENT)
    private val waiter = employee("waiter", "ana Gómez")
    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(admin, waiter)
    private val accounts = FakeAccountCreationRepository()
    private val create = CreateEmployeeUseCase(auth, accounts, users)
    private val update = UpdateEmployeeUseCase(auth, users)

    private suspend fun stored(id: String) = users.observeEmployees().first().first { it.id == id }

    // ---- RF-AUTH-05 and RF-EMP-05

    @Test
    fun anEmployeeSeesOnlyTheOrders() {
        assertEquals(listOf(AppSection.ADD_ORDER, AppSection.CURRENT_ORDERS), menuFor(waiter))
        assertEquals(menuFor(waiter), menuFor(waiter.copy(type = UserType.RECEPTION, area = UserArea.RECEPTION)))
    }

    @Test
    fun anAdministratorSeesEverythingInTheOrderOfTheirArea() {
        assertEquals(
            listOf(
                AppSection.CURRENT_ORDERS,
                AppSection.BALANCE,
                AppSection.INVENTORY,
                AppSection.EMPLOYEES,
                AppSection.SETTINGS,
                AppSection.ADD_ORDER,
            ),
            menuFor(admin),
        )
        assertEquals(AppSection.ADD_ORDER, menuFor(admin.copy(area = UserArea.SERVICE)).first())
        assertEquals(AppSection.entries.toSet(), menuFor(admin.copy(area = UserArea.SERVICE)).toSet())
    }

    @Test
    fun theAreaNeverGivesPermissionsTheRoleDoesNotHave() {
        assertFalse(AppSection.EMPLOYEES in menuFor(waiter.copy(area = UserArea.MANAGEMENT)))
        assertFalse(AppSection.SETTINGS in menuFor(waiter.copy(area = UserArea.MANAGEMENT)))
    }

    // ---- RF-EMP-01

    @Test
    fun employeesAreListedByNameAlsoTheInactiveOnes() = runTest {
        users.save(employee("old", "Zoila Pérez").copy(active = false))

        val names = ObserveEmployeesUseCase(users)().first().map { it.fullName }

        assertEquals(listOf("ana Gómez", "Juan Felipe", "Zoila Pérez"), names)
    }

    // ---- RF-EMP-04

    @Test
    fun createsTheAccountTheProfileAndSendsThePasswordEmail() = runTest {
        val result = create(" Luis Pérez ", " Luis@Hotel.co ", UserType.RECEPTION, UserArea.RECEPTION)

        assertEquals(EmployeeCreation.INVITED, result.getOrNull())
        val created = stored(accounts.accounts.getValue("luis@hotel.co"))
        assertEquals("Luis Pérez" to "luis@hotel.co", created.fullName to created.email)
        assertEquals(UserType.RECEPTION to UserArea.RECEPTION, created.type to created.area)
        assertTrue(created.active)
        assertEquals("admin", users.creators[created.id])
        assertEquals(listOf("luis@hotel.co"), auth.passwordEmails)
    }

    @Test
    fun creatingAnEmployeeDoesNotTouchTheSessionOfTheAdministrator() = runTest {
        create("Luis Pérez", "luis@hotel.co", UserType.BASIC, UserArea.SERVICE)

        assertEquals("admin", auth.currentUserId())
    }

    @Test
    fun anEmployeeNeedsAValidNameAndEmail() = runTest {
        assertEquals(EmployeeError.InvalidName, create("Ana", "ana@hotel.co", UserType.BASIC, UserArea.SERVICE).exceptionOrNull())
        assertEquals(EmployeeError.InvalidEmail, create("Luis Pérez", "luis@", UserType.BASIC, UserArea.SERVICE).exceptionOrNull())
        assertTrue(accounts.accounts.isEmpty())
    }

    @Test
    fun anEmailIsNotRegisteredTwice() = runTest {
        create("Luis Pérez", "luis@hotel.co", UserType.BASIC, UserArea.SERVICE)

        val second = create("Luis Alberto", "LUIS@hotel.co", UserType.BASIC, UserArea.SERVICE)

        assertEquals(EmployeeError.EmailAlreadyInUse, second.exceptionOrNull())
        assertEquals(1, auth.passwordEmails.size)
    }

    @Test
    fun anAccountWhoseProfileCouldNotBeSavedIsRemovedSoTheEmailIsFreeAgain() = runTest {
        users.writeFailure = EmployeeError.Network

        val failed = create("Luis Pérez", "luis@hotel.co", UserType.BASIC, UserArea.SERVICE)

        assertEquals(EmployeeError.Network, failed.exceptionOrNull())
        assertTrue(accounts.accounts.isEmpty())
        assertTrue(auth.passwordEmails.isEmpty())

        users.writeFailure = null
        assertTrue(create("Luis Pérez", "luis@hotel.co", UserType.BASIC, UserArea.SERVICE).isSuccess)
    }

    @Test
    fun theEmployeeStaysCreatedWhenOnlyTheEmailFails() = runTest {
        auth.passwordEmailResult = Result.failure(EmployeeError.Network)

        val result = create("Luis Pérez", "luis@hotel.co", UserType.BASIC, UserArea.SERVICE)

        assertEquals(EmployeeCreation.CREATED_WITHOUT_EMAIL, result.getOrNull())
        assertEquals(1, accounts.accounts.size)
    }

    @Test
    fun nothingIsCreatedWithoutASession() = runTest {
        val withoutSession = CreateEmployeeUseCase(FakeAuthRepository(userId = null), accounts, users)

        val result = withoutSession("Luis Pérez", "luis@hotel.co", UserType.BASIC, UserArea.SERVICE)

        assertEquals(EmployeeError.NoSession, result.exceptionOrNull())
        assertTrue(accounts.accounts.isEmpty())
    }

    // ---- RF-EMP-02, RF-EMP-03 and RF-EMP-05

    @Test
    fun changesTheNameRoleAreaAndAccessOfAnotherEmployee() = runTest {
        val changed = waiter.copy(fullName = " Ana María Gómez ", type = UserType.ADMIN, area = UserArea.HOUSEKEEPING, active = false)

        assertTrue(update(changed).isSuccess)

        assertEquals(changed.copy(fullName = "Ana María Gómez"), stored("waiter"))
    }

    @Test
    fun theEmailOfAnEmployeeNeverChanges() = runTest {
        update(waiter.copy(email = "other@hotel.co"))

        assertEquals(waiter.email, stored("waiter").email)
    }

    @Test
    fun anAdministratorCannotTakeTheirOwnRoleAwayNorDeactivateThemselves() = runTest {
        assertEquals(EmployeeError.OwnAccess, update(admin.copy(type = UserType.BASIC)).exceptionOrNull())
        assertEquals(EmployeeError.OwnAccess, update(admin.copy(active = false)).exceptionOrNull())
        assertEquals(admin, stored("admin"))
    }

    @Test
    fun anAdministratorCanStillChangeTheirOwnNameAndArea() = runTest {
        assertTrue(update(admin.copy(fullName = "Juan Felipe García", area = UserArea.SERVICE)).isSuccess)

        assertEquals("Juan Felipe García" to UserArea.SERVICE, stored("admin").let { it.fullName to it.area })
    }

    @Test
    fun aChangeNeedsAValidNameAndAnExistingEmployee() = runTest {
        assertEquals(EmployeeError.InvalidName, update(waiter.copy(fullName = "Ana")).exceptionOrNull())
        assertEquals(EmployeeError.NotFound, update(employee("gone", "Nadie Existe")).exceptionOrNull())
    }

    // ---- RF-AUTH-04

    @Test
    fun thePasswordEmailGoesToTheCleanAddress() = runTest {
        val result = SendPasswordEmailUseCase(auth)("  Ana@Hotel.CO ")

        assertTrue(result.isSuccess)
        assertEquals(listOf("ana@hotel.co"), auth.passwordEmails)
    }

    @Test
    fun aWrongAddressIsNotSentToFirebase() = runTest {
        assertEquals(EmployeeError.InvalidEmail, SendPasswordEmailUseCase(auth)("ana@").exceptionOrNull())
        assertTrue(auth.passwordEmails.isEmpty())
    }
}
