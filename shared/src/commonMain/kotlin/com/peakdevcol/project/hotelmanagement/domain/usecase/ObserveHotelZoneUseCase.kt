package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** RF-ASI-10: the parameters of the zone of the hotel, as they change. */
class ObserveHotelZoneUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<HotelSettings> = settingsRepository.observeSettings()
}
