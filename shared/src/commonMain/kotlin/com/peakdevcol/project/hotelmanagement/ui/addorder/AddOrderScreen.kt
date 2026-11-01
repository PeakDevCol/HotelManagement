package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.ic_add
import com.peakdevcol.project.hotelmanagement.resources.ic_delete
import com.peakdevcol.project.hotelmanagement.resources.ic_edit
import com.peakdevcol.project.hotelmanagement.resources.menu_add_order
import com.peakdevcol.project.hotelmanagement.resources.order_add_product
import com.peakdevcol.project.hotelmanagement.resources.order_catalog_error
import com.peakdevcol.project.hotelmanagement.resources.order_confirm
import com.peakdevcol.project.hotelmanagement.resources.order_edit_destination
import com.peakdevcol.project.hotelmanagement.resources.order_empty
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.order_error_timeout
import com.peakdevcol.project.hotelmanagement.resources.order_error_title
import com.peakdevcol.project.hotelmanagement.resources.order_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.order_product_line
import com.peakdevcol.project.hotelmanagement.resources.order_remove_product
import com.peakdevcol.project.hotelmanagement.resources.order_saved_message
import com.peakdevcol.project.hotelmanagement.resources.order_saved_title
import com.peakdevcol.project.hotelmanagement.resources.order_total
import com.peakdevcol.project.hotelmanagement.resources.order_unit_price
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddOrderScreen(onBack: () -> Unit, viewModel: AddOrderViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddProduct by rememberSaveable { mutableStateOf(false) }
    var showDestination by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { ScreenTopBar(title = stringResource(Res.string.menu_add_order), onBack = onBack) },
            floatingActionButton = {
                if (state.catalog.isNotEmpty()) {
                    FloatingActionButton(onClick = { showAddProduct = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.order_add_product),
                        )
                    }
                }
            },
            bottomBar = {
                OrderSummary(
                    destination = state.destination.label(),
                    total = formatPesos(state.totalAmount),
                    canConfirm = state.canConfirm,
                    onEditDestination = { showDestination = true },
                    onConfirm = viewModel::onConfirm,
                )
            },
        ) { padding ->
            when {
                state.catalogFailed -> CatalogError(Modifier.padding(padding), onRetry = viewModel::loadCatalog)
                state.products.isEmpty() -> EmptyOrder(Modifier.padding(padding))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 80.dp,
                    ),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductRow(product = product, onRemove = { viewModel.onRemoveProduct(product.id) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        if (state.isSaving) LoadingOverlay()
    }

    if (showAddProduct) {
        AddProductDialog(
            catalog = state.catalog,
            onAdd = { product, quantity ->
                viewModel.onAddProduct(product, quantity)
                showAddProduct = false
            },
            onDismiss = { showAddProduct = false },
        )
    }

    if (showDestination) {
        DestinationDialog(
            current = state.destination,
            onSelect = {
                viewModel.onDestinationSelected(it)
                showDestination = false
            },
            onDismiss = { showDestination = false },
        )
    }

    state.saveResult?.let { result -> SaveResultDialog(result, onDismiss = viewModel::onResultDismissed) }
}

@Composable
private fun ProductRow(product: Product, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.order_product_line, product.name, product.quantity),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(Res.string.order_unit_price, formatPesos(product.unitPrice)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatPesos(product.totalAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.order_remove_product, product.name),
            )
        }
    }
}

@Composable
private fun OrderSummary(
    destination: String,
    total: String,
    canConfirm: Boolean,
    onEditDestination: () -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = destination,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEditDestination) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit),
                        contentDescription = stringResource(Res.string.order_edit_destination),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.order_total, total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(Res.string.order_confirm),
                    onClick = onConfirm,
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EmptyOrder(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.order_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CatalogError(modifier: Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.order_catalog_error), textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
    }
}

@Composable
private fun SaveResultDialog(result: SaveResult, onDismiss: () -> Unit) {
    val (title, message) = when (result) {
        SaveResult.Saved -> Res.string.order_saved_title to Res.string.order_saved_message
        is SaveResult.Failed -> Res.string.order_error_title to when (result.reason) {
            SaveFailure.NoSession -> Res.string.order_error_no_session
            SaveFailure.Timeout -> Res.string.order_error_timeout
            SaveFailure.Unknown -> Res.string.order_error_unknown
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_accept)) }
        },
    )
}
