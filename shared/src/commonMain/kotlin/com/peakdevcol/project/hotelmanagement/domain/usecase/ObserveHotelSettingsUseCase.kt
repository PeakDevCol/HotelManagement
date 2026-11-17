package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettingsDetail
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** RF-EMP-06: the settings of the hotel, with who changed them last. */
class ObserveHotelSettingsUseCase(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Flow<HotelSettingsDetail> =
        combine(settingsRepository.observeSettings(), userRepository.observeEmployees()) { settings, employees ->
            HotelSettingsDetail(
                settings = settings,
                updatedByName = employees.firstOrNull { it.id == settings.updatedBy }?.fullName,
            )
        }
}
