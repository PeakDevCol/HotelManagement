package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkAtReceptionUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkWithPhoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveOpenEntryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveReceptionMarkingUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAttendanceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeDeviceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeLocationProvider
import com.peakdevcol.project.hotelmanagement.fakes.FakeReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeThisDevice
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.fakes.entry
import com.peakdevcol.project.hotelmanagement.fakes.hotelSettings
import com.peakdevcol.project.hotelmanagement.fakes.readingNorthOfHotel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** RF-ASI-03 to RF-ASI-05 and RF-ASI-10 from the phone. */
class MarkWithPhoneUseCaseTest {
    private val now = Instant.parse("2026-11-20T11:58:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val attendance = FakeAttendanceRepository()
    private val settings = FakeSettingsRepository(hotelSettings)
    private val location = FakeLocationProvider()
    private val auth = FakeAuthRepository(userId = "ana")

    private fun markWithPhone() = MarkWithPhoneUseCase(auth, attendance, settings, location, clock)

    @Test
    fun anEntryInsideTheZoneKeepsTheDistanceTheAccuracyAndTheTimeOfThePhone() = runTest {
        location.result = LocationResult.Found(readingNorthOfHotel(degrees = 0.001, accuracyMeters = 18.0))

        val outcome = markWithPhone()(MarkKind.ENTRY).getOrThrow()

        assertEquals(MarkKind.ENTRY to MarkDelivery.SENT, outcome.kind to outcome.delivery)
        val record = attendance.records.value.single()
        assertEquals("ana", record.employeeId)
        assertNull(record.clockIn.at)
        assertEquals(now, record.clockIn.deviceTime)
        assertEquals(MarkSource.PHONE, record.clockIn.source)
        assertEquals(111 to 18, record.clockIn.distanceMeters to record.clockIn.accuracyMeters)
        assertEquals(listOf(100.0), location.requests)
    }

    @Test
    fun withoutConnectionTheMarkWaitsOnThePhone() = runTest {
        attendance.delivery = MarkDelivery.PENDING

        assertEquals(MarkDelivery.PENDING, markWithPhone()(MarkKind.ENTRY).getOrThrow().delivery)
        assertEquals(1, attendance.records.value.size)
    }

    @Test
    fun theExitClosesTheOpenEntryAndSaysTheHoursWorked() = runTest {
        attendance.records.value = listOf(entry("a-1", "ana", at = "2026-11-20T03:00:00Z"))

        val outcome = markWithPhone()(MarkKind.EXIT).getOrThrow()

        assertEquals(8.hours + 58.minutes, outcome.workedTime)
        assertEquals(now, attendance.records.value.single().clockOut?.deviceTime)
    }

    @Test
    fun anotherEntryIsNotMarkedWhileOneIsOpen() = runTest {
        attendance.records.value = listOf(entry("a-1", "ana", at = "2026-11-20T03:00:00Z"))

        assertEquals(AttendanceError.AlreadyOpen, markWithPhone()(MarkKind.ENTRY).exceptionOrNull())
    }

    @Test
    fun thereIsNoExitWithoutAnOpenEntry() = runTest {
        assertEquals(AttendanceError.NoOpenEntry, markWithPhone()(MarkKind.EXIT).exceptionOrNull())
    }

    @Test
    fun anEntryForgottenForLongerThanTheParameterDoesNotBlockTheNextEntry() = runTest {
        attendance.records.value = listOf(entry("yesterday", "ana", at = "2026-11-19T11:00:00Z"))

        markWithPhone()(MarkKind.ENTRY).getOrThrow()

        assertEquals(2, attendance.records.value.size)
        assertTrue(attendance.records.value.first { it.id == "yesterday" }.isOpen)
    }

    @Test
    fun outsideTheZoneItSaysHowFarAndSavesNothing() = runTest {
        location.result = LocationResult.Found(readingNorthOfHotel(degrees = 0.0045))

        assertEquals(AttendanceError.OutsideZone(500), markWithPhone()(MarkKind.ENTRY).exceptionOrNull())
        assertTrue(attendance.records.value.isEmpty())
    }

    @Test
    fun aSimulatedOrImpreciseLocationIsRejected() = runTest {
        location.result = LocationResult.Found(readingNorthOfHotel(simulated = true))
        assertEquals(AttendanceError.SimulatedLocation, markWithPhone()(MarkKind.ENTRY).exceptionOrNull())

        location.result = LocationResult.Found(readingNorthOfHotel(accuracyMeters = 250.0))
        assertEquals(AttendanceError.ImpreciseLocation(250), markWithPhone()(MarkKind.ENTRY).exceptionOrNull())
    }

    @Test
    fun withoutLocationItSaysWhy() = runTest {
        location.result = LocationResult.Disabled
        assertEquals(AttendanceError.LocationDisabled, markWithPhone()(MarkKind.ENTRY).exceptionOrNull())

        location.result = LocationResult.TimedOut
        assertEquals(AttendanceError.LocationTimedOut, markWithPhone()(MarkKind.ENTRY).exceptionOrNull())

        location.result = LocationResult.PermissionDenied
        assertEquals(AttendanceError.LocationPermissionDenied, markWithPhone()(MarkKind.ENTRY).exceptionOrNull())
    }

    @Test
    fun withoutTheZoneOfTheHotelTheLocationIsNotEvenRead() = runTest {
        settings.settings.value = HotelSettings()

        assertEquals(AttendanceError.NoZone, markWithPhone()(MarkKind.ENTRY).exceptionOrNull())
        assertTrue(location.requests.isEmpty())
    }

    @Test
    fun withoutSessionNothingIsRead() = runTest {
        val result = MarkWithPhoneUseCase(FakeAuthRepository(), attendance, settings, location, clock)(MarkKind.ENTRY)

        assertEquals(AttendanceError.NoSession, result.exceptionOrNull())
    }

    @Test
    fun theOpenEntryKnowsWhenItIsForgotten() = runTest {
        attendance.records.value = listOf(entry("a-1", "ana", at = "2026-11-20T11:00:00Z"))

        val open = ObserveOpenEntryUseCase(auth, attendance, settings)().first()!!

        assertEquals("a-1", open.record.id)
        assertEquals(Instant.parse("2026-11-21T03:00:00Z"), open.forgottenAt)
    }
}

/** RF-ASI-11 at the authorized computer of reception. */
class MarkAtReceptionUseCaseTest {
    private val now = Instant.parse("2026-11-20T19:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val ana = employee("ana", "Ana Gómez")
    private val luis = employee("luis", "Luis Pérez").copy(active = false)
    private val attendance = FakeAttendanceRepository()
    private val reception = FakeReceptionMarkingRepository(attendance, "secreta1" to ana, "secreta2" to luis)
    private val devices = FakeDeviceRepository(AuthorizedDevice("reception-pc", "Recepción", "admin"))
    private val settings = FakeSettingsRepository(hotelSettings)

    private fun markAtReception(thisDevice: FakeThisDevice = FakeThisDevice()) = MarkAtReceptionUseCase(reception, devices, settings, thisDevice, clock)

    @Test
    fun withoutAnOpenEntryItMarksTheEntryWithTheComputerAndNoLocation() = runTest {
        val outcome = markAtReception()(" ${ana.email} ", "secreta1").getOrThrow()

        assertEquals(MarkKind.ENTRY, outcome.kind)
        assertEquals("Ana Gómez", outcome.employeeName)
        val mark = attendance.records.value.single().clockIn
        assertEquals(MarkSource.KIOSK to "reception-pc", mark.source to mark.deviceId)
        assertNull(mark.distanceMeters)
        assertNull(reception.signedIn)
    }

    @Test
    fun withAnOpenEntryItMarksTheExit() = runTest {
        attendance.records.value = listOf(entry("a-1", "ana", at = "2026-11-20T11:00:00Z"))

        val outcome = markAtReception()(ana.email, "secreta1").getOrThrow()

        assertEquals(MarkKind.EXIT, outcome.kind)
        assertEquals(8.hours, outcome.workedTime)
        assertFalse(attendance.records.value.single().isOpen)
    }

    @Test
    fun aForgottenEntryIsLeftForTheAdministratorAndANewEntryIsMarked() = runTest {
        attendance.records.value = listOf(entry("yesterday", "ana", at = "2026-11-19T11:00:00Z"))

        assertEquals(MarkKind.ENTRY, markAtReception()(ana.email, "secreta1").getOrThrow().kind)
    }

    @Test
    fun aComputerThatIsNotAuthorizedOrWasRevokedDoesNotMark() = runTest {
        assertEquals(AttendanceError.DeviceNotAuthorized, markAtReception(FakeThisDevice(id = "other-pc"))(ana.email, "secreta1").exceptionOrNull())

        devices.revoke("reception-pc")
        assertEquals(AttendanceError.DeviceNotAuthorized, markAtReception()(ana.email, "secreta1").exceptionOrNull())
        assertTrue(attendance.records.value.isEmpty())
    }

    @Test
    fun aPhoneNeverMarksForOthers() = runTest {
        val phone = FakeThisDevice(id = "reception-pc", canMarkForOthers = false)

        assertEquals(AttendanceError.DeviceNotAuthorized, markAtReception(phone)(ana.email, "secreta1").exceptionOrNull())
    }

    @Test
    fun aWrongPasswordOrAnInactiveEmployeeMarksNothing() = runTest {
        assertEquals(AttendanceError.InvalidCredentials, markAtReception()(ana.email, "otra").exceptionOrNull())
        assertEquals(AttendanceError.InactiveAccount, markAtReception()(luis.email, "secreta2").exceptionOrNull())
        assertTrue(attendance.records.value.isEmpty())
    }

    @Test
    fun theComputerShowsMarkingOnlyWhileItIsAuthorized() = runTest {
        val observe = ObserveReceptionMarkingUseCase(devices, FakeThisDevice())
        assertTrue(observe().first())

        devices.revoke("reception-pc")
        assertFalse(observe().first())

        assertFalse(ObserveReceptionMarkingUseCase(devices, FakeThisDevice(canMarkForOthers = false))().first())
    }

    @Test
    fun withoutReadingTheComputersMarkingIsNotShown() = runTest {
        devices.readFailure = IllegalStateException("permission denied")

        assertFalse(ObserveReceptionMarkingUseCase(devices, FakeThisDevice())().first())
    }
}
