package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSnackbarHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The short messages of the app, like "Pago registrado". They live above the screens, so a message
 * survives going back to the previous screen, as after confirming an order.
 */
class AppMessages(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()

    fun show(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }
}

val LocalAppMessages = staticCompositionLocalOf<AppMessages> { error("AppMessages is missing above this screen") }

/** True on tablets and desktop, where short tasks open as dialogs instead of bottom sheets. */
val LocalWideWindow = compositionLocalOf { false }

/** False for a detail shown beside its list: the list already shows the messages of both. */
val LocalShowsMessages = compositionLocalOf { true }

@Composable
fun rememberAppMessages(): AppMessages {
    val scope = rememberCoroutineScope()
    return remember(scope) { AppMessages(scope) }
}

/** Goes in the snackbar slot of every screen. */
@Composable
fun AppMessagesHost() {
    if (LocalShowsMessages.current) MajesticSnackbarHost(LocalAppMessages.current.hostState)
}
