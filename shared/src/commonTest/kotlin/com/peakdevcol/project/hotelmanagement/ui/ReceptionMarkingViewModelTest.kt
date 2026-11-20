package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkAtReceptionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveReceptionMarkingUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeDeviceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeThisDevice
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.ui.attendance.ReceptionMarkingIntent
import com.peakdevcol.project.hotelmanagement.ui.attendance.ReceptionMarkingViewModel
import com.peakdevcol.project.hotelmanagement.ui.attendance.ReceptionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF-ASI-11. */
class ReceptionMarkingViewModelTest : MainDispatcherTest() {
    private val ana = employee("ana", "Ana Gómez")
    private val attendance = FakeAttendanceRepository()
    private val devices = FakeDeviceRepository(AuthorizedDevice("reception-pc", "Recepción", "admin"))

    private fun viewModel(thisDevice: FakeThisDevice = FakeThisDevice()) = ReceptionMarkingViewModel(
        observeReceptionMarking = ObserveReceptionMarkingUseCase(devices, thisDevice),
        markAtReception = MarkAtReceptionUseCase(
            FakeReceptionMarkingRepository(attendance, "secreta1" to ana),
            devices,
            FakeSettingsRepository(),
            thisDevice,
        ),
    )

    private fun ReceptionMarkingViewModel.type(email: String, password: String) {
        onIntent(ReceptionMarkingIntent.EmailChange(email))
        onIntent(ReceptionMarkingIntent.PasswordChange(password))
    }

    @Test
    fun onlyAnAuthorizedComputerShowsMarking() = runTest {
        assertTrue(viewModel().state.value.isAvailable)
        assertFalse(viewModel(FakeThisDevice(id = "other-pc")).state.value.isAvailable)
        assertFalse(viewModel(FakeThisDevice(id = "other-pc")).state.value.isChecking)

        devices.revoke("reception-pc")
        assertFalse(viewModel().state.value.isAvailable)
    }

    @Test
    fun afterMarkingNothingOfTheEmployeeStaysButWhatHappened() {
        val viewModel = viewModel()
        viewModel.type(ana.email, "secreta1")
        viewModel.onIntent(ReceptionMarkingIntent.TogglePasswordVisibility)

        viewModel.onIntent(ReceptionMarkingIntent.Mark)

        val state = viewModel.state.value
        assertEquals("" to "", state.emailText to state.passwordText)
        assertFalse(state.isPasswordVisible)
        val result = assertIs<ReceptionResult.Done>(state.result)
        assertEquals(MarkKind.ENTRY to "Ana Gómez", result.outcome.kind to result.outcome.employeeName)
        assertEquals(1, attendance.records.value.size)
    }

    @Test
    fun theNextEmployeeTypingClearsTheResult() {
        val viewModel = viewModel()
        viewModel.type(ana.email, "secreta1")
        viewModel.onIntent(ReceptionMarkingIntent.Mark)

        viewModel.onIntent(ReceptionMarkingIntent.EmailChange("l"))

        assertNull(viewModel.state.value.result)
    }

    @Test
    fun aWrongPasswordIsClearedAndTheEmailStays() {
        val viewModel = viewModel()
        viewModel.type(ana.email, "otra")

        viewModel.onIntent(ReceptionMarkingIntent.Mark)

        val state = viewModel.state.value
        assertEquals(ReceptionResult.Failed(AttendanceError.InvalidCredentials), state.result)
        assertEquals(ana.email to "", state.emailText to state.passwordText)
        assertTrue(attendance.records.value.isEmpty())
    }

    @Test
    fun withoutEmailOrPasswordNothingIsSent() {
        val viewModel = viewModel()
        viewModel.type(ana.email, "")

        assertFalse(viewModel.state.value.canMark)
        viewModel.onIntent(ReceptionMarkingIntent.Mark)

        assertNull(viewModel.state.value.result)
    }
}
