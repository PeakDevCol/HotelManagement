package com.peakdevcol.project.hotelmanagement.ui.start

import androidx.compose.foundation.layout.Arrangement
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
import com.peakdevcol.project.hotelmanagement.platformName
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.app_name
import com.peakdevcol.project.hotelmanagement.resources.start_error
import com.peakdevcol.project.hotelmanagement.resources.start_firebase_not_configured
import com.peakdevcol.project.hotelmanagement.resources.start_logged_in
import com.peakdevcol.project.hotelmanagement.resources.start_logged_out
import com.peakdevcol.project.hotelmanagement.resources.start_running_on
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Temporary first screen of the multiplatform app. It proves that DI, Firebase and the shared
 * UI work on every platform. It gets replaced by the login flow in the next phase.
 */
@Composable
fun StartScreen(viewModel: StartViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StartContent(uiState)
}

@Composable
private fun StartContent(uiState: StartUiState) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
            Text(
                text = stringResource(Res.string.start_running_on, platformName()),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (uiState) {
                StartUiState.Loading -> CircularProgressIndicator()
                StartUiState.FirebaseNotConfigured -> StatusText(stringResource(Res.string.start_firebase_not_configured))
                StartUiState.LoggedOut -> StatusText(stringResource(Res.string.start_logged_out))
                is StartUiState.LoggedIn -> StatusText(stringResource(Res.string.start_logged_in, uiState.userId))
                is StartUiState.Error -> StatusText(stringResource(Res.string.start_error, uiState.message))
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}
