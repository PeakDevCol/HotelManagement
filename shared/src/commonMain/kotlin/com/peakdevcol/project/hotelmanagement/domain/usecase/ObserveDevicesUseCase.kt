package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** RF-ASI-11: the authorized computers, the active ones first. */
class ObserveDevicesUseCase(private val deviceRepository: DeviceRepository) {
    operator fun invoke(): Flow<List<AuthorizedDevice>> =
        deviceRepository.observeDevices().map { devices -> devices.sortedWith(compareByDescending<AuthorizedDevice> { it.active }.thenBy { it.name.lowercase() }) }
}
