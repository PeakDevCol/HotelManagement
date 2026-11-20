package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.usecase.AuthorizeThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.IdentifyThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveDevicesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RevokeDeviceUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeDeviceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeThisDevice
import com.peakdevcol.project.hotelmanagement.ui.devices.DevicesEffect
import com.peakdevcol.project.hotelmanagement.ui.devices.DevicesIntent
import com.peakdevcol.project.hotelmanagement.ui.devices.DevicesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF-ASI-11. */
class DevicesViewModelTest : MainDispatcherTest() {
    private val devices = FakeDeviceRepository(AuthorizedDevice("bar-pc", "Bar", "admin"))

    private fun viewModel(thisDevice: FakeThisDevice = FakeThisDevice()) = DevicesViewModel(
        observeDevices = ObserveDevicesUseCase(devices),
        authorizeThisDevice = AuthorizeThisDeviceUseCase(FakeAuthRepository(userId = "admin"), devices, thisDevice),
        revokeDevice = RevokeDeviceUseCase(devices),
        identifyThisDevice = IdentifyThisDeviceUseCase(thisDevice),
    )

    @Test
    fun theDesktopAuthorizesItselfAndSeesIt() = runTest {
        val viewModel = viewModel()
        assertNull(viewModel.state.value.thisDevice)

        viewModel.onIntent(DevicesIntent.NameChange("Recepción"))
        viewModel.onIntent(DevicesIntent.AuthorizeThisDevice)

        assertEquals(DevicesEffect.Authorized, viewModel.effects.first())
        assertEquals("Recepción", viewModel.state.value.thisDevice?.name)
        assertEquals("", viewModel.state.value.nameText)
    }

    @Test
    fun anEmptyNameIsMarkedOnTheField() {
        val viewModel = viewModel()

        viewModel.onIntent(DevicesIntent.AuthorizeThisDevice)

        assertTrue(viewModel.state.value.nameError)
        assertFalse(viewModel.state.value.isWorking)
    }

    @Test
    fun revokingAsksFirst() = runTest {
        val viewModel = viewModel()
        val bar = viewModel.state.value.devices.single()

        viewModel.onIntent(DevicesIntent.Revoke(bar))
        assertEquals(bar, viewModel.state.value.deviceToRevoke)
        viewModel.onIntent(DevicesIntent.ConfirmRevoke)

        assertEquals(DevicesEffect.Revoked, viewModel.effects.first())
        assertNull(viewModel.state.value.deviceToRevoke)
        assertFalse(devices.devices.value.single().active)
    }

    @Test
    fun aPhoneOnlyListsAndRevokes() {
        assertFalse(viewModel(FakeThisDevice(id = "phone", canMarkForOthers = false)).state.value.canAuthorizeThisDevice)
    }

    @Test
    fun aRejectionOfTheServerIsShown() = runTest {
        devices.writeFailure = AttendanceError.NotAllowed
        val viewModel = viewModel()
        viewModel.onIntent(DevicesIntent.Revoke(viewModel.state.value.devices.single()))

        viewModel.onIntent(DevicesIntent.ConfirmRevoke)

        assertEquals(DevicesEffect.Failed(AttendanceError.NotAllowed), viewModel.effects.first())
    }

    @Test
    fun withoutTheListItOffersToRetry() {
        devices.readFailure = IllegalStateException("permission denied")
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.loadFailed)

        devices.readFailure = null
        viewModel.onIntent(DevicesIntent.Retry)

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(1, viewModel.state.value.devices.size)
    }
}
