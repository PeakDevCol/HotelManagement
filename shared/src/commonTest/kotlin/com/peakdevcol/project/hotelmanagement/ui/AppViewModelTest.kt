package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.LogOutUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveAccessUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeOrderRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest : MainDispatcherTest() {

    private val admin = User(
        id = "uid-1",
        fullName = "Juan Felipe",
        email = "admin@hotel.co",
        type = UserType.ADMIN,
        area = UserArea.MANAGEMENT,
        active = true,
    )
    private val users = FakeUserRepository(admin)

    @Test
    fun showsTheLoginWhenThereIsNoSession() = runTest {
        val viewModel = viewModel(FakeAuthRepository(userId = null))

        assertEquals(AppState.LoggedOut, viewModel.state.value)
    }

    @Test
    fun showsTheMenuToAnActiveEmployee() = runTest {
        val viewModel = viewModel(FakeAuthRepository(userId = "uid-1"))

        assertEquals(AppState.LoggedIn(admin), viewModel.state.value)
    }

    @Test
    fun deniesAnAccountWithoutActiveProfile() = runTest {
        val viewModel = viewModel(FakeAuthRepository(userId = "created-outside-the-app"))

        assertEquals(AppState.AccessDenied, viewModel.state.value)
    }

    @Test
    fun deniesTheEmployeeAsSoonAsTheyAreDeactivated() = runTest {
        val viewModel = viewModel(FakeAuthRepository(userId = "uid-1"))

        users.save(admin.copy(active = false))

        assertEquals(AppState.AccessDenied, viewModel.state.value)
    }

    @Test
    fun logOutIntentGoesBackToTheLogin() = runTest {
        val viewModel = viewModel(FakeAuthRepository(userId = "created-outside-the-app"))

        viewModel.onIntent(AppIntent.LogOut)

        assertEquals(AppState.LoggedOut, viewModel.state.value)
    }

    @Test
    fun showsAnErrorWhenTheProfileCannotBeRead() = runTest {
        users.failure = IllegalStateException("permission denied")
        val viewModel = viewModel(FakeAuthRepository(userId = "uid-1"))

        assertIs<AppState.Error>(viewModel.state.value)
    }

    @Test
    fun doesNotTouchFirebaseWhenItIsNotConfigured() = runTest {
        val viewModel = viewModel(FakeAuthRepository(userId = "uid-1"), firebaseEnabled = false)

        assertEquals(AppState.FirebaseNotConfigured, viewModel.state.value)
    }

    private fun TestScope.viewModel(auth: FakeAuthRepository, firebaseEnabled: Boolean = true): AppViewModel {
        val viewModel = AppViewModel(
            config = AppConfig(firebaseEnabled = firebaseEnabled),
            observeAccess = ObserveAccessUseCase(auth, users),
            logOut = LogOutUseCase(auth, FakeOrderRepository()),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }
}
