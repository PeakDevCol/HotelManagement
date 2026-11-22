package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkHousekeepingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomBoardUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomTypesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomTypeUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeRoomTypeRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeTaxRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.room
import com.peakdevcol.project.hotelmanagement.fakes.roomType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** RF-HAB-01 to RF-HAB-04, with decisions 1A, 3A, 4A and 5A. */
class RoomUseCasesTest {
    private val double = roomType("double", "Doble")
    private val suite = roomType("suite", "Suite", capacity = 4, baseRate = 600_000)
    private val oldType = roomType("old", "Sencilla antigua", active = false)
    private val types = FakeRoomTypeRepository(double, suite, oldType)
    private val rooms = FakeRoomRepository(room("r-204", 204), room("r-101", 101, roomTypeId = "suite"), room("r-old", 305, roomTypeId = "old").copy(active = false))
    private val users = FakeUserRepository(employee("admin", "Laura Méndez", UserType.ADMIN), employee("ana", "Ana Rojas"))

    // ---- RF-HAB-01: types

    private val saveType = SaveRoomTypeUseCase(types)

    @Test
    fun aTypeHasANameACapacityAndARateWithTheTaxIncluded() = runTest {
        saveType(typeId = null, name = "  Triple ", capacity = 3, baseRate = 320_000, taxId = "iva").getOrThrow()

        val triple = types.types.value.single { it.name == "Triple" }
        assertEquals(3, triple.capacity)
        assertEquals(320_000, triple.baseRate)
        assertEquals("iva", triple.taxId)
        assertTrue(triple.active)
    }

    @Test
    fun aTypeNeedsANameACapacityOfOneToTwentyAndARate() = runTest {
        assertEquals(RoomError.BlankName, saveType(null, " ", 2, 100_000, null).exceptionOrNull())
        assertEquals(RoomError.InvalidCapacity, saveType(null, "Triple", 0, 100_000, null).exceptionOrNull())
        assertEquals(RoomError.InvalidCapacity, saveType(null, "Triple", 21, 100_000, null).exceptionOrNull())
        assertEquals(RoomError.InvalidCapacity, saveType(null, "Triple", null, 100_000, null).exceptionOrNull())
        assertEquals(RoomError.InvalidRate, saveType(null, "Triple", 3, 0, null).exceptionOrNull())
        assertEquals(RoomError.InvalidRate, saveType(null, "Triple", 3, null, null).exceptionOrNull())
    }

    @Test
    fun twoTypesDoNotShareAName() = runTest {
        assertEquals(RoomError.DuplicateTypeName, saveType(null, "doble", 2, 100_000, null).exceptionOrNull())
        // Its own name is not a repetition.
        assertTrue(saveType("double", "Doble", 3, 280_000, null, active = false).isSuccess)
        assertFalse(types.types.value.single { it.id == "double" }.active)
    }

    @Test
    fun theTypesComeByNameWithTheTaxesOfTheInventory() = runTest {
        val iva = Tax("iva", "IVA", 19_00)
        val catalog = ObserveRoomTypesUseCase(types, FakeTaxRepository(iva)).invoke().first()

        assertEquals(listOf("Doble", "Sencilla antigua", "Suite"), catalog.types.map { it.name })
        assertEquals(iva, catalog.taxOf(double.copy(taxId = "iva")))
    }

    // ---- RF-HAB-02: rooms

    private val saveRoom = SaveRoomUseCase(rooms, types)

    @Test
    fun aNewRoomStartsFreeAndClean() = runTest {
        saveRoom(roomId = null, number = 205, floor = 2, roomTypeId = "double").getOrThrow()

        val created = rooms.created.single()
        assertEquals(205, created.number)
        assertEquals(Occupancy.VACANT, created.occupancy)
        assertEquals(Housekeeping.CLEAN, created.housekeeping)
    }

    @Test
    fun aRoomNeedsANumberAFloorAndAnActiveType() = runTest {
        assertEquals(RoomError.InvalidNumber, saveRoom(null, 0, 2, "double").exceptionOrNull())
        assertEquals(RoomError.InvalidNumber, saveRoom(null, 10_000, 2, "double").exceptionOrNull())
        assertEquals(RoomError.InvalidFloor, saveRoom(null, 205, 100, "double").exceptionOrNull())
        assertEquals(RoomError.InvalidFloor, saveRoom(null, 205, null, "double").exceptionOrNull())
        assertEquals(RoomError.MissingType, saveRoom(null, 205, 2, null).exceptionOrNull())
        assertEquals(RoomError.MissingType, saveRoom(null, 205, 2, "old").exceptionOrNull())
        assertTrue(rooms.created.isEmpty())
    }

    @Test
    fun aNumberIsNotRepeatedEvenByAnInactiveRoom() = runTest {
        assertEquals(RoomError.DuplicateNumber, saveRoom(null, 204, 2, "double").exceptionOrNull())
        assertEquals(RoomError.DuplicateNumber, saveRoom(null, 305, 3, "double").exceptionOrNull())
    }

    @Test
    fun editingARoomChangesItsDataButNeverItsState() = runTest {
        rooms.rooms.value = rooms.rooms.value.map { if (it.id == "r-204") it.copy(housekeeping = Housekeeping.DIRTY) else it }

        saveRoom(roomId = "r-204", number = 214, floor = 2, roomTypeId = "suite", active = false).getOrThrow()

        val edited = rooms.rooms.value.single { it.id == "r-204" }
        assertEquals(214, edited.number)
        assertEquals("suite", edited.roomTypeId)
        assertFalse(edited.active)
        assertEquals(Housekeeping.DIRTY, edited.housekeeping)
    }

    @Test
    fun aRoomKeepsItsInactiveTypeButCannotTakeAnother() = runTest {
        assertTrue(saveRoom("r-old", 306, 3, "old", active = true).isSuccess)
        assertEquals(RoomError.MissingType, saveRoom("r-204", 204, 2, "old").exceptionOrNull())
        assertEquals(RoomError.NotFound, saveRoom("gone", 999, 9, "double").exceptionOrNull())
    }

    // ---- RF-HAB-03: marking the housekeeping by hand

    private val now = Instant.parse("2026-11-21T14:00:00Z")

    private fun markAs(userId: String?) = MarkHousekeepingUseCase(FakeAuthRepository(userId = userId), rooms)

    @Test
    fun anyEmployeeMarksAFreeRoomCleanInTheirName() = runTest {
        val dirty = rooms.rooms.value.first().copy(housekeeping = Housekeeping.DIRTY)
        rooms.rooms.value = listOf(dirty)

        markAs("ana")(dirty, Housekeeping.CLEAN).getOrThrow()

        val marked = rooms.rooms.value.single()
        assertEquals(Housekeeping.CLEAN, marked.housekeeping)
        assertEquals("ana", marked.housekeepingChangedBy)
        assertEquals(now, marked.housekeepingChangedAt)
    }

    @Test
    fun aRoomThatIsNotAsTheEmployeeSawItIsNotMarked() = runTest {
        val clean = rooms.rooms.value.first()

        assertEquals(RoomError.Changed, markAs("ana")(clean, Housekeeping.CLEAN).exceptionOrNull())
        assertEquals(RoomError.Changed, markAs("ana")(clean.copy(occupancy = Occupancy.OCCUPIED), Housekeeping.DIRTY).exceptionOrNull())
        assertEquals(RoomError.NoSession, markAs(null)(clean, Housekeeping.DIRTY).exceptionOrNull())
    }

    // ---- RF-HAB-04: the board

    private fun board(userId: String) = ObserveRoomBoardUseCase(FakeAuthRepository(userId = userId), users, rooms, types)

    @Test
    fun everyEmployeeSeesTheActiveRoomsByFloorAndNumberWithTheirType() = runTest {
        val board = board("ana")().first()

        assertEquals(listOf(101, 204), board.rooms.map { it.room.number })
        assertEquals("Suite", board.rooms.first().type?.name)
        assertTrue(board.inactiveRooms.isEmpty())
        assertFalse(board.canManage)
    }

    @Test
    fun anAdministratorAlsoGetsTheInactiveRoomsToSetThemUp() = runTest {
        val board = board("admin")().first()

        assertTrue(board.canManage)
        assertEquals(listOf(305), board.inactiveRooms.map { it.room.number })
        assertEquals(listOf("Doble", "Sencilla antigua", "Suite"), board.types.map { it.name })
    }

    @Test
    fun theBoardSaysWhoMarkedTheHousekeepingLast() = runTest {
        rooms.rooms.value = listOf(room("r-204", 204).copy(housekeepingChangedBy = "ana", housekeepingChangedAt = now))

        assertEquals("Ana Rojas", board("admin")().first().rooms.single().housekeepingChangedByName)
    }
}
