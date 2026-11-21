package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** RF-ASI-11: whether this computer is authorized now, the only case that shows marking for others. */
class ObserveReceptionMarkingUseCase(
    private val deviceRepository: DeviceRepository,
    private val thisDevice: ThisDevice,
) {
    operator fun invoke(): Flow<Boolean> {
        if (!thisDevice.canMarkForOthers) return flowOf(false)
        return deviceRepository.observeDevice(thisDevice.id)
            .map { device -> device?.active == true }
            .catch { emit(false) }
    }
}
