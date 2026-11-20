package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.runtime.Composable

/**
 * S10: asks the system for the location, only when marking and after the app explains what it is
 * for. The returned function shows the request of the system; [onResult] says if it was granted.
 */
@Composable
expect fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit
