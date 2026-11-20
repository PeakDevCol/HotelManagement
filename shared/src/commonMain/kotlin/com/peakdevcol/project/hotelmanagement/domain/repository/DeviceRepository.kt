package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import kotlinx.coroutines.flow.Flow

/** RF-ASI-11: the computers of reception allowed to mark attendance, at `Devices`. */
interface DeviceRepository {
    /** Every computer, for the administrator. */
    fun observeDevices(): Flow<List<AuthorizedDevice>>

    /** One computer, by its id: what a computer reads to know whether it is authorized. */
    fun observeDevice(deviceId: String): Flow<AuthorizedDevice?>

    suspend fun authorize(device: AuthorizedDevice): Result<Unit>

    /** The device stays, inactive, so the marks made on it still say where they came from. */
    suspend fun revoke(deviceId: String): Result<Unit>
}

/** This installation of the app. */
interface ThisDevice {
    /** The same for this installation every time, so its authorization survives restarting the app. */
    val id: String

    /** RF-ASI-11: only the desktop app is set up at reception to mark for the employees. */
    val canMarkForOthers: Boolean
}
