package com.peakdevcol.project.hotelmanagement.data.location

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult

// A computer has no GPS: there, attendance is marked only on an authorized computer (RF-ASI-11).
private object NoLocationProvider : LocationProvider {
    override val isAvailable = false

    override fun hasPermission() = false

    override suspend fun currentLocation(desiredAccuracyMeters: Double): LocationResult = LocationResult.Unavailable
}

internal actual fun locationProvider(config: AppConfig): LocationProvider = NoLocationProvider

internal actual fun canMarkForOthersOnThisPlatform(): Boolean = true
