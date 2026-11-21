package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocateThisDeviceUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.LocationAccessUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelZoneUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveHotelZoneUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeLocationProvider
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.hotelSettings
import com.peakdevcol.project.hotelmanagement.fakes.readingNorthOfHotel
import com.peakdevcol.project.hotelmanagement.ui.hotelzone.HotelZoneEffect
import com.peakdevcol.project.hotelmanagement.ui.hotelzone.HotelZoneIntent
import com.peakdevcol.project.hotelmanagement.ui.hotelzone.HotelZoneViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RF-ASI-10. */
class HotelZoneViewModelTest : MainDispatcherTest() {
    private val settings = FakeSettingsRepository(HotelSettings())
    private val location = FakeLocationProvider()

    private fun viewModel(location: FakeLocationProvider = this.location) = HotelZoneViewModel(
        observeHotelZone = ObserveHotelZoneUseCase(settings),
        saveHotelZone = SaveHotelZoneUseCase(FakeAuthRepository(userId = "admin"), settings),
        locateThisDevice = LocateThisDeviceUseCase(location),
        locationAccess = LocationAccessUseCase(location),
    )

    @Test
    fun withoutALocationTheFieldsStartEmptyWithTheDefaultSizes() {
        val state = viewModel().state.value

        assertFalse(state.hasZone)
        assertEquals("" to "", state.latitudeText to state.longitudeText)
        assertEquals("150" to "100", state.radiusText to state.accuracyText)
    }

    @Test
    fun theCoordinatesPastedFromAMapFillBothFields() {
        val viewModel = viewModel()

        viewModel.onIntent(HotelZoneIntent.LatitudeChange("4.60971, -74.08175"))

        assertEquals("4.60971" to "-74.08175", viewModel.state.value.latitudeText to viewModel.state.value.longitudeText)
    }

    @Test
    fun savesTheZone() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(HotelZoneIntent.LatitudeChange("4,60971"))
        viewModel.onIntent(HotelZoneIntent.LongitudeChange("-74.08175"))
        viewModel.onIntent(HotelZoneIntent.RadiusChange("200 m"))

        viewModel.onIntent(HotelZoneIntent.Save)

        assertEquals(HotelZoneEffect.Saved, viewModel.effects.first())
        assertEquals(hotelSettings.zone!!.copy(radiusMeters = 200), settings.settings.value.zone)
    }

    @Test
    fun anInvalidZoneMarksTheFieldsInsteadOfAMessage() {
        val viewModel = viewModel()
        viewModel.onIntent(HotelZoneIntent.LatitudeChange("95"))
        viewModel.onIntent(HotelZoneIntent.LongitudeChange("-74"))

        viewModel.onIntent(HotelZoneIntent.Save)

        assertTrue(viewModel.state.value.showError)
        viewModel.onIntent(HotelZoneIntent.LatitudeChange("4.6"))
        assertFalse(viewModel.state.value.showError)
    }

    @Test
    fun myLocationFillsTheFieldsAndWaitsForSave() = runTest {
        location.result = LocationResult.Found(readingNorthOfHotel(accuracyMeters = 8.0))
        val viewModel = viewModel()

        viewModel.onIntent(HotelZoneIntent.UseMyLocation)

        assertEquals(HotelZoneEffect.Located(accuracyMeters = 8), viewModel.effects.first())
        assertEquals("4.60971" to "-74.08175", viewModel.state.value.latitudeText to viewModel.state.value.longitudeText)
        assertEquals(null, settings.settings.value.hotelLatitude)
    }

    @Test
    fun myLocationAsksForThePermissionAfterExplainingIt() = runTest {
        location.permission = false
        val viewModel = viewModel()

        viewModel.onIntent(HotelZoneIntent.UseMyLocation)
        assertTrue(viewModel.state.value.isExplainingPermission)

        viewModel.onIntent(HotelZoneIntent.PermissionResult(granted = false))

        assertFalse(viewModel.state.value.isExplainingPermission)
        assertEquals(HotelZoneEffect.LocateFailed(AttendanceError.LocationPermissionDenied), viewModel.effects.first())
    }

    @Test
    fun theDesktopCannotUseItsLocation() {
        assertFalse(viewModel(FakeLocationProvider(isAvailable = false)).state.value.canUseMyLocation)
    }

    @Test
    fun aRejectionOfTheServerIsShown() = runTest {
        settings.saveFailure = SettingsError.NotAllowed
        val viewModel = viewModel()
        viewModel.onIntent(HotelZoneIntent.LatitudeChange("4.60971, -74.08175"))

        viewModel.onIntent(HotelZoneIntent.Save)

        assertEquals(HotelZoneEffect.SaveFailed(SettingsError.NotAllowed), viewModel.effects.first())
    }
}
