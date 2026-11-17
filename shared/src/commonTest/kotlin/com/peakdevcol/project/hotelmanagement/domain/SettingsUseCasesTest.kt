package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatTimeOfDay
import com.peakdevcol.project.hotelmanagement.domain.model.parseBogotaDate
import com.peakdevcol.project.hotelmanagement.domain.usecase.ExportBackupUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveHotelSettingsUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveOperationalDayStartUseCase
import com.peakdevcol.project.hotelmanagement.fakes.FakeAuthRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeBackupRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeSettingsRepository
import com.peakdevcol.project.hotelmanagement.fakes.FakeUserRepository
import com.peakdevcol.project.hotelmanagement.fakes.employee
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SettingsUseCasesTest {
    private val auth = FakeAuthRepository(userId = "admin")
    private val users = FakeUserRepository(employee("admin", "Juan Felipe", type = UserType.ADMIN))
    private val settings = FakeSettingsRepository()
    private val save = SaveOperationalDayStartUseCase(auth, settings)

    // ---- RF-EMP-06

    @Test
    fun theOperationalDayStartsAtSixUntilSomeoneChangesIt() = runTest {
        val detail = ObserveHotelSettingsUseCase(settings, users)().first()

        assertEquals(6 * 60, detail.settings.operationalDayStartMinutes)
        assertNull(detail.settings.updatedAt)
        assertNull(detail.updatedByName)
    }

    @Test
    fun aChangeIsSavedWithWhoMadeItAndWhen() = runTest {
        assertTrue(save(hour = 5, minute = 30).isSuccess)

        val detail = ObserveHotelSettingsUseCase(settings, users)().first()
        assertEquals(HotelSettings(operationalDayStartMinutes = 330, updatedBy = "admin", updatedAt = settings.now), detail.settings)
        assertEquals("Juan Felipe", detail.updatedByName)
    }

    @Test
    fun midnightAndTheLastMinuteOfTheDayAreValid() = runTest {
        assertTrue(save(hour = 0, minute = 0).isSuccess)
        assertTrue(save(hour = 23, minute = 59).isSuccess)

        assertEquals(1_439, settings.settings.value.operationalDayStartMinutes)
    }

    @Test
    fun aTimeThatDoesNotExistIsRejected() = runTest {
        listOf(24 to 0, 6 to 60, -1 to 0, null to 0, 6 to null).forEach { (hour, minute) ->
            assertEquals(SettingsError.InvalidTime, save(hour, minute).exceptionOrNull(), "$hour:$minute")
        }
        assertEquals(HotelSettings(), settings.settings.value)
    }

    @Test
    fun nothingIsSavedWithoutASession() = runTest {
        val result = SaveOperationalDayStartUseCase(FakeAuthRepository(userId = null), settings)(6, 0)

        assertEquals(SettingsError.NoSession, result.exceptionOrNull())
    }

    @Test
    fun showsATimeOfTheDayTheColombianWay() {
        assertEquals("6:00 a. m.", formatTimeOfDay(360))
        assertEquals("12:00 a. m.", formatTimeOfDay(0))
        assertEquals("12:05 p. m.", formatTimeOfDay(725))
        assertEquals("11:59 p. m.", formatTimeOfDay(1_439))
    }

    // ---- RF-EMP-07

    @Test
    fun readsADateAsTheStartOfThatDayInColombia() {
        assertEquals(Instant.parse("2026-11-01T05:00:00Z"), parseBogotaDate("2026-11-01"))
        assertEquals(Instant.parse("2028-02-29T05:00:00Z"), parseBogotaDate(" 2028-02-29 "))
        assertEquals("1 nov 2026, 12:00 a. m.", formatBogotaDateTime(parseBogotaDate("2026-11-01")!!))
    }

    @Test
    fun rejectsWhatIsNotARealDate() {
        listOf("", "2026-13-01", "2026-02-30", "2027-02-29", "01-11-2026x", "2026/11/01", "2026-11", "hoy", "2026-00-10", "2026-11-00").forEach {
            assertNull(parseBogotaDate(it), it)
        }
    }

    @Test
    fun aBackupWithoutDateExportsEverything() = runTest {
        val backups = FakeBackupRepository()

        val summary = ExportBackupUseCase(backups)("  ").getOrThrow()

        assertEquals(listOf<Instant?>(null), backups.exports)
        assertEquals(42, summary.documentCount)
    }

    @Test
    fun aBackupFromADateStartsAtTheBeginningOfThatColombianDay() = runTest {
        val backups = FakeBackupRepository()

        ExportBackupUseCase(backups)("2026-11-01")

        assertEquals(listOf<Instant?>(Instant.parse("2026-11-01T05:00:00Z")), backups.exports)
    }

    @Test
    fun aWrongDateDoesNotStartABackup() = runTest {
        val backups = FakeBackupRepository()

        assertEquals(BackupError.InvalidDate, ExportBackupUseCase(backups)("ayer").exceptionOrNull())
        assertTrue(backups.exports.isEmpty())
    }

    @Test
    fun onlyTheDesktopAppMakesBackups() = runTest {
        val phone = ExportBackupUseCase(FakeBackupRepository(isAvailable = false))

        assertEquals(false, phone.isAvailable)
        assertEquals(BackupError.NotAvailable, phone("").exceptionOrNull())
    }
}
