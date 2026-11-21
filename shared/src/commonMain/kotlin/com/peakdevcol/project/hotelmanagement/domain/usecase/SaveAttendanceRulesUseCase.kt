package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** RF-EMP-06: an administrator changes the late arrival tolerance and the hours of an open entry. */
class SaveAttendanceRulesUseCase(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(lateToleranceMinutes: Int?, maxOpenEntryHours: Int?): Result<Unit> {
        if (lateToleranceMinutes == null || lateToleranceMinutes !in HotelSettings.LATE_TOLERANCE_RANGE ||
            maxOpenEntryHours == null || maxOpenEntryHours !in HotelSettings.MAX_OPEN_ENTRY_HOURS_RANGE
        ) {
            return Result.failure(SettingsError.InvalidAttendanceRules)
        }
        val administratorId = authRepository.currentUserId() ?: return Result.failure(SettingsError.NoSession)
        // The other parameters stay as they are: the document is written whole.
        val current = settingsRepository.observeSettings().first()
        return settingsRepository.save(
            current.copy(lateToleranceMinutes = lateToleranceMinutes, maxOpenEntryHours = maxOpenEntryHours),
            updatedBy = administratorId,
        )
    }
}
