package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.CopyPreviousWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteShiftUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveEmployeesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveScheduleWeekUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveShiftUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeShiftRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.shift
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesEffect
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesIntent
import com.peakdevcol.project.hotelmanagement.ui.schedules.SchedulesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** RF-ASI-01. */
class SchedulesViewModelTest : MainDispatcherTest() {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-20T15:00:00Z")
    }
    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(
        employee("ana", "Ana Gómez"),
        employee("admin", "Juan Felipe", type = UserType.ADMIN),
        employee("old", "Zoila Pérez").copy(active = false),
    )
    private val shifts = FakeShiftRepository(
        shift("morning", "ana", "2026-11-16", "06:00", "14:00"),
        shift("afternoon", "ana", "2026-11-16", "13:00", "21:00"),
        shift("last-week", "ana", "2026-11-12", "06:00", "14:00"),
    )

    private fun viewModel() = SchedulesViewModel(
        observeScheduleWeek = ObserveScheduleWeekUseCase(shifts),
        observeEmployees = ObserveEmployeesUseCase(users),
        saveShift = SaveShiftUseCase(auth, shifts),
        deleteShift = DeleteShiftUseCase(shifts),
        copyPreviousWeek = CopyPreviousWeekUseCase(auth, shifts),
        clock = clock,
    )

    @Test
    fun opensOnThisWeekAndWarnsAboutTheShiftsThatCross() {
        val state = viewModel().state.value

        assertEquals(CalendarDate(2026, 11, 16), state.monday)
        assertFalse(state.isLoading)
        assertEquals(listOf("morning", "afternoon"), state.days.first().shifts.map { it.shift.id })
        assertEquals(2, state.overlapCount)
        assertEquals("Ana Gómez", state.nameOf("ana"))
        assertEquals(listOf("ana", "admin"), state.activeEmployees.map { it.id })
    }

    @Test
    fun movesBetweenWeeks() {
        val viewModel = viewModel()

        viewModel.onIntent(SchedulesIntent.PreviousWeek)
        assertEquals(CalendarDate(2026, 11, 9), viewModel.state.value.monday)
        assertEquals(listOf("last-week"), viewModel.state.value.days.flatMap { it.shifts }.map { it.shift.id })

        viewModel.onIntent(SchedulesIntent.ThisWeek)
        assertEquals(CalendarDate(2026, 11, 16), viewModel.state.value.monday)
    }

    @Test
    fun createsAShiftOnTheDayItCameFrom() = runTest {
        val viewModel = viewModel()
        val friday = CalendarDate(2026, 11, 20)

        viewModel.onIntent(SchedulesIntent.NewShift(friday))
        viewModel.onIntent(SchedulesIntent.SelectEmployee("ana"))
        viewModel.onIntent(SchedulesIntent.StartChange("22:00"))
        viewModel.onIntent(SchedulesIntent.EndChange("0600"))
        viewModel.onIntent(SchedulesIntent.SelectArea(UserArea.RECEPTION))
        viewModel.onIntent(SchedulesIntent.SaveShift)

        assertNull(viewModel.state.value.form)
        assertEquals(SchedulesEffect.Saved, viewModel.effects.first())
        val created = viewModel.state.value.days.first { it.date == friday }.shifts.single().shift
        assertEquals(Instant.parse("2026-11-21T11:00:00Z"), created.endAt)
        assertEquals(UserArea.RECEPTION, created.area)
    }

    @Test
    fun aWrongTimeKeepsTheFormOpenWithItsError() {
        val viewModel = viewModel()
        viewModel.onIntent(SchedulesIntent.NewShift())
        viewModel.onIntent(SchedulesIntent.SelectEmployee("ana"))
        viewModel.onIntent(SchedulesIntent.StartChange("25:00"))
        viewModel.onIntent(SchedulesIntent.EndChange("06:00"))

        viewModel.onIntent(SchedulesIntent.SaveShift)

        assertEquals(ScheduleError.InvalidTime, viewModel.state.value.form?.error)
        assertEquals(3, shifts.shifts.value.size)
    }

    @Test
    fun editingFillsTheFormAndDeletingAsksFirst() = runTest {
        val viewModel = viewModel()
        val morning = viewModel.state.value.days.first().shifts.first().shift

        viewModel.onIntent(SchedulesIntent.EditShift(morning))
        val form = assertNotNull(viewModel.state.value.form)
        assertEquals("06:00", form.startText)
        assertEquals("14:00", form.endText)

        viewModel.onIntent(SchedulesIntent.DeleteShift)
        assertTrue(viewModel.state.value.isConfirmingDelete)
        viewModel.onIntent(SchedulesIntent.ConfirmDelete)

        assertEquals(SchedulesEffect.Deleted, viewModel.effects.first())
        assertFalse(shifts.shifts.value.any { it.id == "morning" })
        assertEquals(0, viewModel.state.value.overlapCount)
    }

    @Test
    fun copiesThePreviousWeekAfterConfirming() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(SchedulesIntent.CopyPreviousWeek)
        assertTrue(viewModel.state.value.isConfirmingCopy)
        viewModel.onIntent(SchedulesIntent.ConfirmCopy)

        assertEquals(SchedulesEffect.Copied(1), viewModel.effects.first())
        assertEquals(3, viewModel.state.value.days.sumOf { it.shifts.size })
    }

    @Test
    fun showsAnErrorAndRetries() {
        shifts.readFailure = IllegalStateException("permission denied")
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.loadFailed)

        shifts.readFailure = null
        viewModel.onIntent(SchedulesIntent.Retry)

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(2, viewModel.state.value.days.first().shifts.size)
    }
}
