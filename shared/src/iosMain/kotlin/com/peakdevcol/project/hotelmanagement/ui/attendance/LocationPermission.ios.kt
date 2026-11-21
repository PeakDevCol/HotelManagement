package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onResult)
    val requester = remember { LocationPermissionRequester() }
    DisposableEffect(requester) { onDispose { requester.forget() } }
    return { requester.request { granted -> latest(granted) } }
}

/** Asks "while using the app" only: the location is read only at the moment of marking (S10). */
private class LocationPermissionRequester : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var pending: ((Boolean) -> Unit)? = null

    fun request(onResult: (Boolean) -> Unit) {
        val status = manager.authorizationStatus
        // iOS asks only once: after a "No", the person changes it in the settings of the phone.
        if (status != kCLAuthorizationStatusNotDetermined) {
            onResult(status.isGranted())
            return
        }
        pending = onResult
        manager.delegate = this
        manager.requestWhenInUseAuthorization()
    }

    fun forget() {
        pending = null
        manager.delegate = null
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusNotDetermined) return
        pending?.invoke(status.isGranted())
        pending = null
    }

    private fun CLAuthorizationStatus.isGranted() =
        this == kCLAuthorizationStatusAuthorizedWhenInUse || this == kCLAuthorizationStatusAuthorizedAlways
}
