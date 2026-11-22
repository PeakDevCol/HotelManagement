package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkHousekeepingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomBoardUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomTypeRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.room
import com.peakdevcol.project.hotelmanagement.fakes.roomType
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomForm
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomsEffect
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomsIntent
import com.peakdevcol.project.hotelmanagement.ui.rooms.RoomsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RF-HAB-02 to RF-HAB-04, with decisions 1A, 4A and 5A. */
class RoomsViewModelTest : MainDispatcherTest() {
    private val double = roomType("double", "Doble")
    private val types = FakeRoomTypeRepository(double, roomType("old", "Sencilla", active = false))
    private val rooms = FakeRoomRepository(
        room("r-101", 101),
        room("r-204", 204).copy(housekeeping = Housekeeping.DIRTY),
        room("r-205", 205),
        room("r-305", 305).copy(active = false),
    )
    private val users = FakeUserRepository(employee("admin", "Laura Méndez", UserType.ADMIN), employee("ana", "Ana Rojas"))

    private fun viewModel(userId: String = "ana"): RoomsViewModel {
        val auth = FakeAuthRepository(userId = userId)
        return RoomsViewModel(
            observeRoomBoard = ObserveRoomBoardUseCase(auth, users, rooms, types),
            markHousekeeping = MarkHousekeepingUseCase(auth, rooms),
            saveRoom = SaveRoomUseCase(rooms, types),
        )
    }

    @Test
    fun theBoardShowsTheActiveRoomsByFloorLive() {
        val viewModel = viewModel()

        assertEquals(listOf(1, 2), viewModel.state.value.floors.map { it.floor })

        rooms.rooms.value += room("r-401", 401)

        assertEquals(listOf(1, 2, 4), viewModel.state.value.floors.map { it.floor })
    }

    @Test
    fun aFilterShowsTheRoomsInOneStateAndEachChipCountsItsOwn() {
        val viewModel = viewModel()

        viewModel.onIntent(RoomsIntent.SelectFilter(RoomStatus.DIRTY))

        val state = viewModel.state.value
        assertEquals(listOf(204), state.floors.flatMap { it.rooms }.map { it.room.number })
        assertEquals(3, state.countOf(null))
        assertEquals(2, state.countOf(RoomStatus.AVAILABLE))

        viewModel.onIntent(RoomsIntent.SelectFilter(RoomStatus.DIRTY))
        assertNull(viewModel.state.value.filter)
    }

    @Test
    fun anEmployeeMarksARoomCleanAndTheDetailCloses() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(RoomsIntent.OpenRoom("r-204"))

        viewModel.onIntent(RoomsIntent.MarkHousekeeping("r-204", Housekeeping.CLEAN))

        assertEquals(RoomsEffect.Marked(204, Housekeeping.CLEAN), viewModel.effects.first())
        assertNull(viewModel.state.value.openRoomId)
        assertEquals(RoomStatus.AVAILABLE, viewModel.state.value.board.roomOf("r-204")?.status)
        assertEquals("Ana Rojas", viewModel.state.value.board.roomOf("r-204")?.housekeepingChangedByName)
    }

    @Test
    fun aMarkTheServerRejectsKeepsTheDetailOpenAndSaysWhy() = runTest {
        rooms.markFailure = RoomError.Changed
        val viewModel = viewModel()
        viewModel.onIntent(RoomsIntent.OpenRoom("r-204"))

        viewModel.onIntent(RoomsIntent.MarkHousekeeping("r-204", Housekeeping.CLEAN))

        assertEquals(RoomsEffect.Failed(RoomError.Changed), viewModel.effects.first())
        assertEquals("r-204", viewModel.state.value.openRoomId)
        assertNull(viewModel.state.value.markingRoomId)
    }

    @Test
    fun onlyAnAdministratorSetsUpRoomsAndSeesTheInactiveOnes() {
        val employee = viewModel("ana")
        employee.onIntent(RoomsIntent.AddRoom)
        employee.onIntent(RoomsIntent.EditRoom("r-101"))

        assertFalse(employee.state.value.canAddRooms)
        assertNull(employee.state.value.form)
        assertTrue(employee.state.value.board.inactiveRooms.isEmpty())

        val admin = viewModel("admin")
        assertTrue(admin.state.value.canAddRooms)
        assertEquals(listOf(305), admin.state.value.board.inactiveRooms.map { it.room.number })
    }

    @Test
    fun anAdministratorAddsARoomWithTheOnlyActiveTypeChosen() {
        val viewModel = viewModel("admin")

        viewModel.onIntent(RoomsIntent.AddRoom)
        assertEquals(RoomForm(roomTypeId = "double"), viewModel.state.value.form)
        viewModel.onIntent(RoomsIntent.NumberChange("2a06"))
        viewModel.onIntent(RoomsIntent.FloorChange("2"))
        viewModel.onIntent(RoomsIntent.SaveRoom)

        assertNull(viewModel.state.value.form)
        assertEquals(206, rooms.created.single().number)
        assertEquals(listOf(204, 205, 206), viewModel.state.value.floors.last().rooms.map { it.room.number })
    }

    @Test
    fun aRepeatedNumberKeepsTheFormOpenWithItsError() {
        val viewModel = viewModel("admin")
        viewModel.onIntent(RoomsIntent.AddRoom)
        viewModel.onIntent(RoomsIntent.NumberChange("204"))
        viewModel.onIntent(RoomsIntent.FloorChange("2"))

        viewModel.onIntent(RoomsIntent.SaveRoom)

        assertEquals(RoomError.DuplicateNumber, viewModel.state.value.form?.error)
        viewModel.onIntent(RoomsIntent.NumberChange("207"))
        assertNull(viewModel.state.value.form?.error)
    }

    @Test
    fun editingARoomOpensItsDataInTheForm() {
        val viewModel = viewModel("admin")
        viewModel.onIntent(RoomsIntent.OpenRoom("r-305"))

        viewModel.onIntent(RoomsIntent.EditRoom("r-305"))

        val state = viewModel.state.value
        assertNull(state.openRoomId)
        assertEquals(RoomForm(roomId = "r-305", numberText = "305", floorText = "3", roomTypeId = "double", active = false), state.form)
        assertEquals(listOf("double"), state.selectableTypes.map { it.id })
    }

    @Test
    fun withoutActiveTypesTheAdministratorIsSentToCreateOne() = runTest {
        types.types.value = listOf(roomType("old", "Sencilla", active = false))
        val viewModel = viewModel("admin")

        viewModel.onIntent(RoomsIntent.AddRoom)
        viewModel.onIntent(RoomsIntent.ManageTypes)

        assertTrue(viewModel.state.value.needsTypes)
        assertNull(viewModel.state.value.form)
        assertEquals(RoomsEffect.NavigateToRoomTypes, viewModel.effects.first())
    }

    @Test
    fun aBoardThatCannotBeReadSaysSoAndRetries() {
        rooms.readFailure = IllegalStateException("PERMISSION_DENIED")
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.loadFailed)

        rooms.readFailure = null
        viewModel.onIntent(RoomsIntent.Retry)

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(3, viewModel.state.value.board.rooms.size)
    }
}
