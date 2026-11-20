package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.LocationReading
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider

// The hotel is the center of the zone of everyone, so it is fixed with a good reading.
private const val HOTEL_FIX_ACCURACY_METERS = 30.0

/** RF-ASI-10: "Usar mi ubicación actual", for the administrator standing at the hotel. */
class LocateThisDeviceUseCase(private val locationProvider: LocationProvider) {
    suspend operator fun invoke(): Result<LocationReading> {
        val reading = locationProvider.currentLocation(HOTEL_FIX_ACCURACY_METERS).toReading().getOrElse { return Result.failure(it) }
        return if (reading.isSimulated) Result.failure(AttendanceError.SimulatedLocation) else Result.success(reading)
    }
}
