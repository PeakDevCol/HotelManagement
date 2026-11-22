package com.peakdevcol.project.hotelmanagement.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.access_denied_message
import com.peakdevcol.project.hotelmanagement.resources.access_denied_title
import com.peakdevcol.project.hotelmanagement.resources.ds_hotel_name
import com.peakdevcol.project.hotelmanagement.resources.firebase_not_configured
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.session_error
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.login.LoginScreen
import com.peakdevcol.project.hotelmanagement.ui.main.MainScreen
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Not private: on the JVM, navigation reads the serializer of a route by reflection, and a private class refuses it (RNF-01).
@Serializable
internal data object LoginRoute

@Serializable
internal data object SessionRoute

@Composable
fun App() {
    MajesticTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MajesticTheme.colors.surface, contentColor = MajesticTheme.colors.ink) {
            val appViewModel: AppViewModel = koinViewModel()
            val appState by appViewModel.state.collectAsStateWithLifecycle()

            when (val state = appState) {
                AppState.Loading -> MajesticLoading()
                // RF-AUTH-03: without a session the app opens on the login (PRD, section 13, 2026-11-20).
                AppState.LoggedOut -> LoginNavigation()
                is AppState.LoggedIn -> SessionNavigation()
                AppState.AccessDenied -> AccessDeniedScreen(onLogOutClick = { appViewModel.onIntent(AppIntent.LogOut) })
                AppState.FirebaseNotConfigured -> MessageScreen(stringResource(Res.string.firebase_not_configured))
                is AppState.Error -> MessageScreen(stringResource(Res.string.session_error, state.message))
            }
        }
    }
}

// A navigation of their own gives the login and the session their view models: they survive
// turning the device, and the next login starts clean, without what the last one typed.
@Composable
private fun LoginNavigation() {
    NavHost(navController = rememberNavController(), startDestination = LoginRoute) {
        composable<LoginRoute> { LoginScreen() }
    }
}

@Composable
private fun SessionNavigation() {
    NavHost(navController = rememberNavController(), startDestination = SessionRoute) {
        composable<SessionRoute> { MainScreen() }
    }
}

/** RF-AUTH-07: the account has a session but no active employee profile. */
@Composable
private fun AccessDeniedScreen(onLogOutClick: () -> Unit) {
    CenteredPage {
        EmptyState(
            icon = MajesticIcons.block,
            title = stringResource(Res.string.access_denied_title),
            message = stringResource(Res.string.access_denied_message),
            actionText = stringResource(Res.string.logout),
            actionIcon = MajesticIcons.logOut,
            onAction = onLogOutClick,
        )
    }
}

@Composable
private fun MessageScreen(message: String) {
    CenteredPage {
        EmptyState(icon = MajesticIcons.error, title = stringResource(Res.string.ds_hotel_name), message = message)
    }
}

@Composable
private fun CenteredPage(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        content()
    }
}
