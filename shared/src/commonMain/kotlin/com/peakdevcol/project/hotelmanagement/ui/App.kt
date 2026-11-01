package com.peakdevcol.project.hotelmanagement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.peakdevcol.project.hotelmanagement.platformName
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.app_name
import com.peakdevcol.project.hotelmanagement.resources.firebase_not_configured
import com.peakdevcol.project.hotelmanagement.resources.running_on
import com.peakdevcol.project.hotelmanagement.resources.session_error
import com.peakdevcol.project.hotelmanagement.ui.components.AuthFrame
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderScreen
import com.peakdevcol.project.hotelmanagement.ui.home.ComingSoonScreen
import com.peakdevcol.project.hotelmanagement.ui.home.HomeScreen
import com.peakdevcol.project.hotelmanagement.ui.home.HomeSection
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionScreen
import com.peakdevcol.project.hotelmanagement.ui.login.LoginScreen
import com.peakdevcol.project.hotelmanagement.ui.signup.SignUpScreen
import com.peakdevcol.project.hotelmanagement.ui.theme.HotelManagementTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Navigation destinations. Each migrated screen adds its own route.
@Serializable
data object IntroductionRoute

@Serializable
data object LoginRoute

@Serializable
data object SignUpRoute

@Serializable
data object HomeRoute

@Serializable
data object AddOrderRoute

/** A menu section that is not migrated yet. */
@Serializable
data class ComingSoonRoute(val section: HomeSection)

@Composable
fun App() {
    HotelManagementTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val appViewModel: AppViewModel = koinViewModel()
            val session by appViewModel.sessionState.collectAsStateWithLifecycle()

            when (val state = session) {
                SessionState.Loading -> LoadingScreen()
                SessionState.LoggedOut -> AuthNavigation()
                is SessionState.LoggedIn -> HomeNavigation()
                SessionState.FirebaseNotConfigured ->
                    MessageScreen(stringResource(Res.string.firebase_not_configured))
                is SessionState.Error ->
                    MessageScreen(stringResource(Res.string.session_error, state.message))
            }
        }
    }
}

/** Screens available without a session. Logging in swaps to [HomeNavigation] automatically. */
@Composable
private fun AuthNavigation() {
    val navController = rememberNavController()
    AuthFrame {
        NavHost(navController = navController, startDestination = IntroductionRoute) {
            composable<IntroductionRoute> {
                IntroductionScreen(
                    onLoginClick = { navController.navigate(LoginRoute) },
                    onSignUpClick = { navController.navigate(SignUpRoute) },
                )
            }
            composable<LoginRoute> {
                LoginScreen(onBack = { navController.popBackStack() })
            }
            composable<SignUpRoute> {
                SignUpScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Screens for a logged-in employee. Logging out swaps back to [AuthNavigation]. */
@Composable
private fun HomeNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(onSectionClick = { section ->
                when (section) {
                    HomeSection.AddOrder -> navController.navigate(AddOrderRoute)
                    else -> navController.navigate(ComingSoonRoute(section))
                }
            })
        }
        composable<AddOrderRoute> {
            AddOrderScreen(onBack = { navController.popBackStack() })
        }
        composable<ComingSoonRoute> { entry ->
            ComingSoonScreen(section = entry.toRoute<ComingSoonRoute>().section, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = stringResource(Res.string.running_on, platformName()))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
