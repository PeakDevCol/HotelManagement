package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.home_coming_soon
import com.peakdevcol.project.hotelmanagement.resources.home_title
import com.peakdevcol.project.hotelmanagement.resources.ic_logout
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.logout_message
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Temporary home. The real menu (orders, inventory, balance) is migrated in the next phase. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    var showLogOutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_title)) },
                actions = {
                    IconButton(onClick = { showLogOutDialog = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_logout),
                            contentDescription = stringResource(Res.string.logout),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.home_coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showLogOutDialog) {
        AlertDialog(
            onDismissRequest = { showLogOutDialog = false },
            title = { Text(stringResource(Res.string.logout)) },
            text = { Text(stringResource(Res.string.logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogOutDialog = false
                    viewModel.onLogOutConfirmed()
                }) { Text(stringResource(Res.string.dialog_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogOutDialog = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}
