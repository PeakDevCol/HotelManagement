package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** RF-ASI-10: an administrator fixes where the hotel is, how far from it a phone marks, and the accuracy it needs. */
class SaveHotelZoneUseCase(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(latitude: Double?, longitude: Double?, radiusMeters: Int?, minAccuracyMeters: Int?): Result<Unit> {
        val valid = latitude != null && latitude in -90.0..90.0 &&
            longitude != null && longitude in -180.0..180.0 &&
            radiusMeters != null && radiusMeters in HotelSettings.ZONE_RADIUS_RANGE &&
            minAccuracyMeters != null && minAccuracyMeters in HotelSettings.MIN_ACCURACY_RANGE
        if (!valid) return Result.failure(SettingsError.InvalidZone)
        val administratorId = authRepository.currentUserId() ?: return Result.failure(SettingsError.NoSession)
        // The other parameters stay as they are: the document is written whole.
        val current = settingsRepository.observeSettings().first()
        return settingsRepository.save(
            current.copy(hotelLatitude = latitude, hotelLongitude = longitude, zoneRadiusMeters = radiusMeters, minAccuracyMeters = minAccuracyMeters),
            updatedBy = administratorId,
        )
    }
}
