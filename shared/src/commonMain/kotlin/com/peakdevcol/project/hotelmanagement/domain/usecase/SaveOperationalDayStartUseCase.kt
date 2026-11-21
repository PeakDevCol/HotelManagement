package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** RF-EMP-06: an administrator changes the time the operational day starts. */
class SaveOperationalDayStartUseCase(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) {
    /** The time on a 24-hour clock. */
    suspend operator fun invoke(hour: Int?, minute: Int?): Result<Unit> {
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return Result.failure(SettingsError.InvalidTime)
        }
        val administratorId = authRepository.currentUserId() ?: return Result.failure(SettingsError.NoSession)
        // The other parameters stay as they are: the document is written whole.
        val current = settingsRepository.observeSettings().first()
        return settingsRepository.save(current.copy(operationalDayStartMinutes = hour * 60 + minute), updatedBy = administratorId)
    }
}
