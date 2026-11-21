package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.location_explanation_continue
import com.peakdevcol.project.hotelmanagement.resources.location_explanation_message
import com.peakdevcol.project.hotelmanagement.resources.location_explanation_not_now
import com.peakdevcol.project.hotelmanagement.resources.location_explanation_title
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ConfirmDialog
import org.jetbrains.compose.resources.stringResource

/**
 * S10: before the system asks for the location, the app says what it is for and that only the
 * distance to the hotel is kept. "Continuar" opens the request of the system, whose answer goes to
 * [onPermissionResult].
 */
@Composable
fun LocationExplanation(onPermissionResult: (granted: Boolean) -> Unit, onDismiss: () -> Unit) {
    val requestPermission = rememberLocationPermissionRequest(onPermissionResult)
    ConfirmDialog(
        title = stringResource(Res.string.location_explanation_title),
        message = stringResource(Res.string.location_explanation_message),
        confirmText = stringResource(Res.string.location_explanation_continue),
        dismissText = stringResource(Res.string.location_explanation_not_now),
        onConfirm = requestPermission,
        onDismiss = onDismiss,
    )
}
