package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice

/** This installation, and whether it can be the computer of reception (RF-ASI-11). */
data class DeviceIdentity(val id: String, val canMarkForOthers: Boolean)

class IdentifyThisDeviceUseCase(private val thisDevice: ThisDevice) {
    operator fun invoke(): DeviceIdentity = DeviceIdentity(thisDevice.id, thisDevice.canMarkForOthers)
}
