package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.home_pending_orders
import com.peakdevcol.project.hotelmanagement.resources.home_title
import com.peakdevcol.project.hotelmanagement.resources.ic_logout
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.logout_message
import com.peakdevcol.project.hotelmanagement.resources.logout_pending_message
import com.peakdevcol.project.hotelmanagement.resources.logout_pending_title
import com.peakdevcol.project.hotelmanagement.resources.menu_add_order
import com.peakdevcol.project.hotelmanagement.resources.menu_balance
import com.peakdevcol.project.hotelmanagement.resources.menu_current_orders
import com.peakdevcol.project.hotelmanagement.resources.menu_inventory
import com.peakdevcol.project.hotelmanagement.resources.order_product_line
import com.peakdevcol.project.hotelmanagement.resources.rejected_additions_message
import com.peakdevcol.project.hotelmanagement.resources.rejected_additions_title
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.ui.addorder.label
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Sections reachable from the main menu, in the order the buttons are shown. */
enum class HomeSection { Balance, Inventory, AddOrder, CurrentOrders }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSectionClick: (HomeSection) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_title)) },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(HomeIntent.LogOutClick) }) {
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
            if (state.pendingOrderCount > 0) {
                PendingOrdersNotice(count = state.pendingOrderCount)
            }
            HomeSection.entries.forEach { section ->
                PrimaryButton(
                    text = stringResource(section.title()),
                    onClick = { onSectionClick(section) },
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            }
        }
    }

    when (state.logOutDialog) {
        LogOutDialog.Confirmation -> LogOutConfirmationDialog(
            onConfirm = { viewModel.onIntent(HomeIntent.LogOutConfirm) },
            onDismiss = { viewModel.onIntent(HomeIntent.LogOutDialogDismiss) },
        )
        LogOutDialog.PendingOrders -> PendingOrdersDialog(
            onDismiss = { viewModel.onIntent(HomeIntent.LogOutDialogDismiss) },
        )
        null -> Unit
    }

    if (state.rejectedAdditions.isNotEmpty()) {
        RejectedAdditionsDialog(
            additions = state.rejectedAdditions,
            onRead = { viewModel.onIntent(HomeIntent.RejectedAdditionsRead) },
        )
    }
}

/** RF-CUR-13: tells which products the server did not add, because their order was no longer in progress. */
@Composable
private fun RejectedAdditionsDialog(additions: List<PendingAddition>, onRead: () -> Unit) {
    AlertDialog(
        onDismissRequest = onRead,
        title = { Text(stringResource(Res.string.rejected_additions_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.rejected_additions_message))
                additions.forEach { addition ->
                    Column {
                        Text(text = addition.destination.label(), style = MaterialTheme.typography.titleSmall)
                        addition.products.forEach { product ->
                            Text(stringResource(Res.string.order_product_line, product.name, product.quantity))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onRead) { Text(stringResource(Res.string.dialog_accept)) } },
    )
}

/** RF-ORD-09: orders saved on the device that the server does not have yet. */
@Composable
private fun PendingOrdersNotice(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        Text(
            text = pluralStringResource(Res.plurals.home_pending_orders, count, count),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun LogOutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.logout)) },
        text = { Text(stringResource(Res.string.logout_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.dialog_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

/** RF-AUTH-03: logging out is not allowed while orders wait to be sent. */
@Composable
private fun PendingOrdersDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.logout_pending_title)) },
        text = { Text(stringResource(Res.string.logout_pending_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_accept)) }
        },
    )
}

fun HomeSection.title() = when (this) {
    HomeSection.Balance -> Res.string.menu_balance
    HomeSection.Inventory -> Res.string.menu_inventory
    HomeSection.AddOrder -> Res.string.menu_add_order
    HomeSection.CurrentOrders -> Res.string.menu_current_orders
}
