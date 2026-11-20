package com.peakdevcol.project.hotelmanagement.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.model.LocationReading
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal actual fun locationProvider(config: AppConfig): LocationProvider =
    AndroidLocationProvider(config.platformContext as Context)

internal actual fun canMarkForOthersOnThisPlatform(): Boolean = false

private val LOCATION_PERMISSIONS = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * RF-ASI-10 with the location service of Android itself, not Google Play Services (2A): the GPS
 * outdoors and the network indoors, whichever answers better first.
 */
private class AndroidLocationProvider(
    private val context: Context,
    private val timeout: Duration = 20.seconds,
) : LocationProvider {

    private val manager get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override val isAvailable = true

    override fun hasPermission(): Boolean =
        LOCATION_PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(desiredAccuracyMeters: Double): LocationResult {
        if (!hasPermission()) return LocationResult.PermissionDenied
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) return LocationResult.Disabled

        var best: Location? = null
        val accurateEnough = withTimeoutOrNull(timeout) {
            fixes(providers).first { fix ->
                if (best == null || fix.accuracy < best!!.accuracy) best = fix
                fix.accuracy <= desiredAccuracyMeters
            }
        }
        val fix = accurateEnough ?: best ?: return LocationResult.TimedOut
        return LocationResult.Found(LocationReading(fix.latitude, fix.longitude, fix.accuracy.toDouble(), fix.isSimulated()))
    }

    // The updates stop as soon as nobody listens: there is no tracking outside the moment of marking (RF-ASI-10).
    @SuppressLint("MissingPermission")
    private fun fixes(providers: List<String>) = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            // Before Android 10 these have no default body, and a missing one crashes the app.
            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("Deprecated in Android 10, still called before it")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) }
        awaitClose { manager.removeUpdates(listener) }
    }

    @Suppress("DEPRECATION")
    private fun Location.isSimulated(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider
}
