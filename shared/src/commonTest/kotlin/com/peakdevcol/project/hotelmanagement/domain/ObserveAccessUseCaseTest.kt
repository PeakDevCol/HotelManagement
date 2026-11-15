package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.Access
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveAccessUseCaseTest {

    private val waiter = User(
        id = "uid-1",
        fullName = "Ana Gómez",
        email = "ana@hotel.co",
        type = UserType.BASIC,
        area = UserArea.SERVICE,
        active = true,
    )

    @Test
    fun thereIsNoAccessWithoutASession() = runTest {
        val access = accessHistory(FakeAuthRepository(userId = null), FakeUserRepository(waiter))

        assertEquals(listOf<Access>(Access.NoSession), access)
    }

    @Test
    fun anActiveEmployeeGetsIn() = runTest {
        val access = accessHistory(FakeAuthRepository(userId = "uid-1"), FakeUserRepository(waiter))

        assertEquals(listOf(Access.Checking, Access.Granted(waiter)), access)
    }

    @Test
    fun anAccountWithoutProfileIsDenied() = runTest {
        val access = accessHistory(FakeAuthRepository(userId = "created-outside-the-app"), FakeUserRepository(waiter))

        assertEquals(Access.Denied, access.last())
    }

    @Test
    fun anInactiveEmployeeIsDenied() = runTest {
        val access = accessHistory(
            FakeAuthRepository(userId = "uid-1"),
            FakeUserRepository(waiter.copy(active = false)),
        )

        assertEquals(Access.Denied, access.last())
    }

    @Test
    fun deactivatingAnEmployeeTakesTheAccessAwayAtOnce() = runTest {
        val users = FakeUserRepository(waiter)
        val access = mutableListOf<Access>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveAccessUseCase(FakeAuthRepository(userId = "uid-1"), users)().toList(access)
        }

        users.save(waiter.copy(active = false))

        assertEquals(listOf(Access.Checking, Access.Granted(waiter), Access.Denied), access)
    }

    @Test
    fun loggingOutEndsTheAccess() = runTest {
        val auth = FakeAuthRepository(userId = "uid-1")
        val access = mutableListOf<Access>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveAccessUseCase(auth, FakeUserRepository(waiter))().toList(access)
        }

        auth.logOut()

        assertEquals(Access.NoSession, access.last())
    }

    private fun TestScope.accessHistory(
        auth: FakeAuthRepository,
        users: FakeUserRepository,
    ): List<Access> {
        val access = mutableListOf<Access>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveAccessUseCase(auth, users)().toList(access)
        }
        return access
    }
}
