package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider

/** What this device can do with the location right now. */
enum class LocationAccess {
    /** No location here, like the desktop app. */
    UNAVAILABLE,

    /** S10: the system has to be asked first, after the app explains why. */
    NEEDS_PERMISSION,
    GRANTED,
}

class LocationAccessUseCase(private val locationProvider: LocationProvider) {
    operator fun invoke(): LocationAccess = when {
        !locationProvider.isAvailable -> LocationAccess.UNAVAILABLE
        locationProvider.hasPermission() -> LocationAccess.GRANTED
        else -> LocationAccess.NEEDS_PERMISSION
    }
}
