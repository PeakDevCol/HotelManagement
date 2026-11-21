package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveWhoIsWorkingUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeTeamAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.bogotaInstant
import com.peakdevcol.project.hotelmanagement.fakes.bogotaRecord
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.ui.attendance.WhoIsWorkingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** RF-ASI-09. */
class WhoIsWorkingViewModelTest : MainDispatcherTest() {
    private fun bogota(text: String) = Instant.parse(bogotaInstant(text))

    private val now = bogota("2026-11-21 10:00")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val minuteTicks = MutableStateFlow(now)
    private val users = FakeUserRepository(
        employee("recepcion", "Rosa Díaz", UserType.RECEPTION),
        employee("ana", "Ana Rojas"),
        employee("luis", "Luis Pérez"),
    )
    private val attendance = FakeAttendanceRepository(
        bogotaRecord("a-1", "ana", "2026-11-21 06:00"),
        bogotaRecord("a-2", "luis", "2026-11-19 06:00"),
        bogotaRecord("a-3", "luis", "2026-11-21 06:00", "2026-11-21 09:00"),
    )
    private val team = FakeTeamAttendanceRepository(attendance)

    private fun viewModel(userId: String) = WhoIsWorkingViewModel(
        observeWhoIsWorking = ObserveWhoIsWorkingUseCase(FakeAuthRepository(userId = userId), users, team, FakeSettingsRepository()),
        clock = clock,
        minuteTicks = minuteTicks,
    )

    @Test
    fun receptionSeesWhoHasTheEntryMarkedButNotTheForgottenOnes() {
        val state = viewModel("recepcion").state.value

        assertTrue(state.isVisible)
        assertEquals(listOf("a-1"), state.working.map { it.entry.record.id })
        assertEquals("Ana Rojas", state.working.single().employeeName)
    }

    @Test
    fun anEntryLeavesTheListWhenItBecomesForgotten() {
        val viewModel = viewModel("recepcion")

        minuteTicks.value = bogota("2026-11-21 22:00")

        assertTrue(viewModel.state.value.working.isEmpty())
    }

    @Test
    fun aBasicEmployeeDoesNotSeeIt() {
        assertFalse(viewModel("ana").state.value.isVisible)
    }

    @Test
    fun aListThatCannotBeReadSaysSo() {
        team.listenFailure = IllegalStateException("PERMISSION_DENIED")

        val state = viewModel("recepcion").state.value

        assertTrue(state.isVisible)
        assertTrue(state.loadFailed)
    }
}
