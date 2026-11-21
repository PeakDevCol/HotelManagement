package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository

/** RF-ASI-11: an administrator takes away the authorization of a computer, from any device. */
class RevokeDeviceUseCase(private val deviceRepository: DeviceRepository) {
    suspend operator fun invoke(deviceId: String): Result<Unit> = deviceRepository.revoke(deviceId)
}
