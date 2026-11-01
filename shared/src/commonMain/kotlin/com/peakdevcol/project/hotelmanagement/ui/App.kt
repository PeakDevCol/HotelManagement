package com.peakdevcol.project.hotelmanagement.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peakdevcol.project.hotelmanagement.ui.start.StartScreen
import com.peakdevcol.project.hotelmanagement.ui.theme.HotelManagementTheme
import kotlinx.serialization.Serializable

/** Navigation destinations. Each migrated screen adds its own route here. */
@Serializable
data object StartRoute

@Composable
fun App() {
    HotelManagementTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = StartRoute) {
            composable<StartRoute> { StartScreen() }
        }
    }
}
