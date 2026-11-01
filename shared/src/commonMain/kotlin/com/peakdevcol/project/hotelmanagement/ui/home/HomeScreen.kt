package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.home_title
import com.peakdevcol.project.hotelmanagement.resources.ic_logout
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.logout_message
import com.peakdevcol.project.hotelmanagement.resources.menu_add_order
import com.peakdevcol.project.hotelmanagement.resources.menu_balance
import com.peakdevcol.project.hotelmanagement.resources.menu_current_orders
import com.peakdevcol.project.hotelmanagement.resources.menu_inventory
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Sections reachable from the main menu, in the order the buttons are shown. */
enum class HomeSection { Balance, Inventory, AddOrder, CurrentOrders }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSectionClick: (HomeSection) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeSection.entries.forEach { section ->
                PrimaryButton(
                    text = stringResource(section.title()),
                    onClick = { onSectionClick(section) },
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            }
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

fun HomeSection.title() = when (this) {
    HomeSection.Balance -> Res.string.menu_balance
    HomeSection.Inventory -> Res.string.menu_inventory
    HomeSection.AddOrder -> Res.string.menu_add_order
    HomeSection.CurrentOrders -> Res.string.menu_current_orders
}
