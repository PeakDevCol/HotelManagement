package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.usecase.ExportBackupUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelSettingsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveAttendanceRulesUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOperationalDayStartUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeBackupRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsEffect
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsIntent
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest : MainDispatcherTest() {
    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(employee("admin", "Juan Felipe", type = UserType.ADMIN))
    private val settings = FakeSettingsRepository()
    private var backups = FakeBackupRepository()

    private fun viewModel() = SettingsViewModel(
        ObserveHotelSettingsUseCase(settings, users),
        SaveOperationalDayStartUseCase(auth, settings),
        SaveAttendanceRulesUseCase(auth, settings),
        ExportBackupUseCase(backups),
    )

    @Test
    fun showsTheCurrentStartOfTheOperationalDay() {
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals("6" to "00", state.hourText to state.minuteText)
    }

    @Test
    fun savesANewStartAndShowsWhoChangedIt() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(SettingsIntent.HourChange("5"))
        viewModel.onIntent(SettingsIntent.MinuteChange("30"))

        viewModel.onIntent(SettingsIntent.SaveOperationalDayStart)

        assertEquals(SettingsEffect.Saved, viewModel.effects.first())
        val detail = viewModel.state.value.detail!!
        assertEquals(330, detail.settings.operationalDayStartMinutes)
        assertEquals("Juan Felipe", detail.updatedByName)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun aWrongTimeIsMarkedAndNotSaved() {
        val viewModel = viewModel()
        viewModel.onIntent(SettingsIntent.HourChange("25"))

        viewModel.onIntent(SettingsIntent.SaveOperationalDayStart)
        assertTrue(viewModel.state.value.showTimeError)
        assertEquals(HotelSettings(), settings.settings.value)

        viewModel.onIntent(SettingsIntent.HourChange("7"))
        assertFalse(viewModel.state.value.showTimeError)
    }

    @Test
    fun theTimeFieldsOnlyTakeTwoDigits() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.HourChange("1a2b3"))

        assertEquals("12", viewModel.state.value.hourText)
    }

    @Test
    fun aChangeMadeOnAnotherDeviceDoesNotOverwriteWhatIsBeingTyped() {
        val viewModel = viewModel()
        viewModel.onIntent(SettingsIntent.HourChange("9"))

        settings.settings.value = HotelSettings(operationalDayStartMinutes = 240)

        assertEquals("9", viewModel.state.value.hourText)
        assertEquals(240, viewModel.state.value.detail?.settings?.operationalDayStartMinutes)
    }

    @Test
    fun aRejectedChangeIsToldInAMessage() = runTest {
        settings.saveFailure = SettingsError.NotAllowed
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SaveOperationalDayStart)

        assertEquals(SettingsEffect.SaveFailed(SettingsError.NotAllowed), viewModel.effects.first())
    }

    @Test
    fun exportsABackupAndShowsWhereItWasSaved() {
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.backupAvailable)

        viewModel.onIntent(SettingsIntent.ExportBackup)

        val backup = viewModel.state.value.lastBackup!!
        assertEquals(42 to "/respaldos/respaldo-1.json", backup.documentCount to backup.filePath)
        assertFalse(viewModel.state.value.isExporting)
    }

    @Test
    fun aWrongBackupDateIsMarkedAndNothingIsExported() {
        val viewModel = viewModel()
        viewModel.onIntent(SettingsIntent.BackupSinceChange("01/11/2026"))

        viewModel.onIntent(SettingsIntent.ExportBackup)

        assertTrue(viewModel.state.value.showBackupDateError)
        assertNull(viewModel.state.value.lastBackup)
        assertTrue(backups.exports.isEmpty())
    }

    @Test
    fun aFailedBackupIsToldInAMessage() = runTest {
        backups.failure = BackupError.Network
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ExportBackup)

        assertEquals(SettingsEffect.BackupFailed(BackupError.Network), viewModel.effects.first())
        assertNull(viewModel.state.value.lastBackup)
    }

    @Test
    fun outsideTheDesktopTheBackupIsNotOffered() {
        backups = FakeBackupRepository(isAvailable = false)
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ExportBackup)

        assertFalse(viewModel.state.value.backupAvailable)
        assertTrue(backups.exports.isEmpty())
    }

    @Test
    fun showsAndSavesTheAttendanceRules() = runTest {
        val viewModel = viewModel()
        assertEquals("10", viewModel.state.value.toleranceText)
        assertEquals("16", viewModel.state.value.maxOpenHoursText)

        viewModel.onIntent(SettingsIntent.ToleranceChange("15 min"))
        viewModel.onIntent(SettingsIntent.MaxOpenHoursChange("12"))
        viewModel.onIntent(SettingsIntent.SaveAttendanceRules)

        assertEquals(15, settings.settings.value.lateToleranceMinutes)
        assertEquals(12, settings.settings.value.maxOpenEntryHours)
        assertEquals(SettingsEffect.Saved, viewModel.effects.first())
    }

    @Test
    fun attendanceRulesOutOfRangeShowTheirError() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.MaxOpenHoursChange("30"))
        viewModel.onIntent(SettingsIntent.SaveAttendanceRules)

        assertTrue(viewModel.state.value.showAttendanceError)
        assertEquals(16, settings.settings.value.maxOpenEntryHours)
    }

    @Test
    fun theZoneAndTheComputersOpenInTheirOwnScreens() = runTest {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.OpenHotelZone)
        assertEquals(SettingsEffect.NavigateToHotelZone, viewModel.effects.first())

        viewModel.onIntent(SettingsIntent.OpenDevices)
        assertEquals(SettingsEffect.NavigateToDevices, viewModel.effects.first())
    }
}
