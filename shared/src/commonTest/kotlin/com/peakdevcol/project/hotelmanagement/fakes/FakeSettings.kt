package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class FakeSettingsRepository(settings: HotelSettings = HotelSettings()) : SettingsRepository {
    val settings = MutableStateFlow(settings)
    var saveFailure: SettingsError? = null
    var now: Instant = Instant.parse("2026-11-17T15:00:00Z")

    override fun observeSettings(): Flow<HotelSettings> = settings

    override suspend fun save(settings: HotelSettings, updatedBy: String): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        this.settings.value = settings.copy(updatedBy = updatedBy, updatedAt = now)
        return Result.success(Unit)
    }
}

class FakeBackupRepository(override val isAvailable: Boolean = true) : BackupRepository {
    /** The "since" of every backup that was asked for. */
    val exports = mutableListOf<Instant?>()
    var failure: BackupError? = null

    override suspend fun export(since: Instant?): Result<BackupSummary> {
        failure?.let { return Result.failure(it) }
        exports += since
        return Result.success(BackupSummary(filePath = "/respaldos/respaldo-${exports.size}.json", documentCount = 42))
    }
}
