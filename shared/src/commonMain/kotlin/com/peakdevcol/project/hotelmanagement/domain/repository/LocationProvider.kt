package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.LocationReading

sealed interface LocationResult {
    data class Found(val reading: LocationReading) : LocationResult
    data object PermissionDenied : LocationResult
    data object Disabled : LocationResult
    data object Unavailable : LocationResult
    data object TimedOut : LocationResult
}

/**
 * RF-ASI-10 and S10: where the device is, read once, only when marking or when the administrator
 * fixes the hotel. It uses the location service of each system, without libraries (2A).
 */
interface LocationProvider {
    /** False where there is no location, like the desktop app. */
    val isAvailable: Boolean

    fun hasPermission(): Boolean

    /**
     * Listens until a reading is as accurate as [desiredAccuracyMeters], or until the time runs out,
     * and then gives the best one it got.
     */
    suspend fun currentLocation(desiredAccuracyMeters: Double): LocationResult
}
