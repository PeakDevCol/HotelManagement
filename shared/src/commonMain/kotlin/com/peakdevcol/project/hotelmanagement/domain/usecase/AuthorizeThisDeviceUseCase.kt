package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice

private const val MAX_DEVICE_NAME_LENGTH = 40

/** RF-ASI-11: an administrator, at the computer of reception, allows it to mark attendance. */
class AuthorizeThisDeviceUseCase(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val thisDevice: ThisDevice,
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        if (!thisDevice.canMarkForOthers) return Result.failure(AttendanceError.DeviceNotAuthorized)
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_DEVICE_NAME_LENGTH) return Result.failure(AttendanceError.InvalidName)
        val administratorId = authRepository.currentUserId() ?: return Result.failure(AttendanceError.NoSession)
        return deviceRepository.authorize(AuthorizedDevice(id = thisDevice.id, name = trimmed, authorizedBy = administratorId))
    }
}
