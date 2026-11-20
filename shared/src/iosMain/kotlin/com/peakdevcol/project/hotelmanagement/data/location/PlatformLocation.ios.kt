package com.peakdevcol.project.hotelmanagement.data.location

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.model.LocationReading
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal actual fun locationProvider(config: AppConfig): LocationProvider = IosLocationProvider()

internal actual fun canMarkForOthersOnThisPlatform(): Boolean = false

/** RF-ASI-10 with Core Location, without libraries (2A). */
private class IosLocationProvider(private val timeout: Duration = 20.seconds) : LocationProvider {

    override val isAvailable = true

    override fun hasPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    }

    override suspend fun currentLocation(desiredAccuracyMeters: Double): LocationResult {
        // Asking it on the main thread makes the screen wait, as Apple warns.
        if (!withContext(Dispatchers.Default) { CLLocationManager.locationServicesEnabled() }) return LocationResult.Disabled
        if (!hasPermission()) return LocationResult.PermissionDenied
        return withContext(Dispatchers.Main) {
            var best: CLLocation? = null
            val accurateEnough = withTimeoutOrNull(timeout) {
                fixes().first { fix ->
                    val current = best
                    if (current == null || fix.horizontalAccuracy < current.horizontalAccuracy) best = fix
                    fix.horizontalAccuracy <= desiredAccuracyMeters
                }
            }
            val fix = accurateEnough ?: best ?: return@withContext LocationResult.TimedOut
            LocationResult.Found(fix.toReading())
        }
    }

    // The updates stop as soon as nobody listens: there is no tracking outside the moment of marking (RF-ASI-10).
    private fun fixes() = callbackFlow {
        val manager = CLLocationManager()
        // The manager keeps its delegate weakly: this flow holds it until it closes.
        val delegate = LocationDelegate { location -> trySend(location) }
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.startUpdatingLocation()
        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun CLLocation.toReading(): LocationReading {
        val (latitude, longitude) = coordinate.useContents { latitude to longitude }
        // V5: iOS 15 and later tell a location faked by software. The app needs iOS 15.3.
        val simulated = sourceInformation?.isSimulatedBySoftware ?: false
        return LocationReading(latitude, longitude, horizontalAccuracy, simulated)
    }
}

private class LocationDelegate(private val onLocation: (CLLocation) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let(onLocation)
    }
}
