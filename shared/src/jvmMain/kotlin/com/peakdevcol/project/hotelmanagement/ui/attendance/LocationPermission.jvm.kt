package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

// A computer has no location to ask for.
@Composable
actual fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onResult)
    return { latest(false) }
}
