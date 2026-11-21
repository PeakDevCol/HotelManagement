package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import com.peakdevcol.project.hotelmanagement.domain.usecase.AuthorizeThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocateThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccess
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveDevicesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.RevokeDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveHotelZoneUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeDeviceRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeLocationProvider
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeThisDevice
import com.peakdevcol.project.hotelmanagement.fakes.readingNorthOfHotel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-ASI-10: the zone of the hotel. */
class HotelZoneUseCasesTest {
    private val settings = FakeSettingsRepository(HotelSettings(operationalDayStartMinutes = 300, lateToleranceMinutes = 15))
    private val saveHotelZone = SaveHotelZoneUseCase(FakeAuthRepository(userId = "admin"), settings)

    @Test
    fun theZoneIsSavedAndTheOtherParametersStay() = runTest {
        saveHotelZone(4.60971, -74.08175, 200, 50).getOrThrow()

        val saved = settings.settings.value
        assertEquals(4.60971 to -74.08175, saved.hotelLatitude to saved.hotelLongitude)
        assertEquals(200 to 50, saved.zoneRadiusMeters to saved.minAccuracyMeters)
        assertEquals(300 to 15, saved.operationalDayStartMinutes to saved.lateToleranceMinutes)
        assertEquals("admin", saved.updatedBy)
    }

    @Test
    fun valuesOutOfRangeAreNotSaved() = runTest {
        assertEquals(SettingsError.InvalidZone, saveHotelZone(null, -74.0, 150, 100).exceptionOrNull())
        assertEquals(SettingsError.InvalidZone, saveHotelZone(91.0, -74.0, 150, 100).exceptionOrNull())
        assertEquals(SettingsError.InvalidZone, saveHotelZone(4.6, -181.0, 150, 100).exceptionOrNull())
        assertEquals(SettingsError.InvalidZone, saveHotelZone(4.6, -74.0, 19, 100).exceptionOrNull())
        assertEquals(SettingsError.InvalidZone, saveHotelZone(4.6, -74.0, 150, 501).exceptionOrNull())
        assertEquals(null, settings.settings.value.hotelLatitude)
    }

    @Test
    fun theLocationOfTheHotelIsTakenWithAGoodReadingAndNeverASimulatedOne() = runTest {
        val location = FakeLocationProvider()
        assertEquals(readingNorthOfHotel(), LocateThisDeviceUseCase(location)().getOrThrow())
        assertEquals(listOf(30.0), location.requests)

        location.result = LocationResult.Found(readingNorthOfHotel(simulated = true))
        assertEquals(AttendanceError.SimulatedLocation, LocateThisDeviceUseCase(location)().exceptionOrNull())
    }

    @Test
    fun theAccessToTheLocationSaysWhetherToAskFirst() {
        assertEquals(LocationAccess.UNAVAILABLE, LocationAccessUseCase(FakeLocationProvider(isAvailable = false))())
        assertEquals(LocationAccess.NEEDS_PERMISSION, LocationAccessUseCase(FakeLocationProvider(permission = false))())
        assertEquals(LocationAccess.GRANTED, LocationAccessUseCase(FakeLocationProvider())())
    }
}

/** RF-ASI-11: the authorized computers. */
class DeviceUseCasesTest {
    private val devices = FakeDeviceRepository()
    private val auth = FakeAuthRepository(userId = "admin")

    @Test
    fun theDesktopAuthorizesItselfWithAName() = runTest {
        AuthorizeThisDeviceUseCase(auth, devices, FakeThisDevice())("  Recepción principal ").getOrThrow()

        assertEquals(listOf(AuthorizedDevice("reception-pc", "Recepción principal", "admin")), devices.devices.value)
    }

    @Test
    fun theNameIsNeededAndShort() = runTest {
        val authorize = AuthorizeThisDeviceUseCase(auth, devices, FakeThisDevice())

        assertEquals(AttendanceError.InvalidName, authorize("   ").exceptionOrNull())
        assertEquals(AttendanceError.InvalidName, authorize("R".repeat(41)).exceptionOrNull())
        assertTrue(devices.devices.value.isEmpty())
    }

    @Test
    fun aPhoneIsNeverAuthorized() = runTest {
        val result = AuthorizeThisDeviceUseCase(auth, devices, FakeThisDevice(canMarkForOthers = false))("Mi celular")

        assertEquals(AttendanceError.DeviceNotAuthorized, result.exceptionOrNull())
    }

    @Test
    fun aRevokedComputerStaysListedAfterTheActiveOnes() = runTest {
        devices.devices.value = listOf(
            AuthorizedDevice("pc-1", "Recepción", "admin"),
            AuthorizedDevice("pc-2", "Bar", "admin"),
        )

        RevokeDeviceUseCase(devices)("pc-1").getOrThrow()

        assertEquals(listOf("pc-2" to true, "pc-1" to false), ObserveDevicesUseCase(devices)().first().map { it.id to it.active })
    }
}
