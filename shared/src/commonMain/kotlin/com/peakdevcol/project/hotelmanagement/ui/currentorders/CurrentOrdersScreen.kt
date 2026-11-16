package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.OrderFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.model.OrderOwnerFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.current_orders_empty
import com.peakdevcol.project.hotelmanagement.resources.current_orders_empty_filter
import com.peakdevcol.project.hotelmanagement.resources.current_orders_error
import com.peakdevcol.project.hotelmanagement.resources.filter_all_orders
import com.peakdevcol.project.hotelmanagement.resources.filter_all_states
import com.peakdevcol.project.hotelmanagement.resources.filter_my_orders
import com.peakdevcol.project.hotelmanagement.resources.menu_current_orders
import com.peakdevcol.project.hotelmanagement.ui.addorder.label
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurrentOrdersScreen(
    onBack: () -> Unit,
    onOrderClick: (orderId: String) -> Unit,
    viewModel: CurrentOrdersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CurrentOrdersEffect.NavigateToOrder -> onOrderClick(effect.orderId)
            }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.menu_current_orders), onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OrderFilters(filter = state.filter, onIntent = viewModel::onIntent)
            HorizontalDivider()
            when {
                state.isLoading -> Loading()
                state.loadFailed -> CenteredMessage(
                    message = stringResource(Res.string.current_orders_error),
                    onRetry = { viewModel.onIntent(CurrentOrdersIntent.Retry) },
                )
                state.orders.isEmpty() -> CenteredMessage(stringResource(Res.string.current_orders_empty))
                state.visibleOrders.isEmpty() -> CenteredMessage(stringResource(Res.string.current_orders_empty_filter))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.visibleOrders, key = { it.order.id }) { listing ->
                        OrderRow(
                            listing = listing,
                            onClick = { viewModel.onIntent(CurrentOrdersIntent.OpenOrder(listing.order.id)) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderFilters(filter: OrderFilter, onIntent: (CurrentOrdersIntent) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        FilterRow {
            FilterChip(
                selected = filter.owner == OrderOwnerFilter.ALL,
                onClick = { onIntent(CurrentOrdersIntent.SelectOwner(OrderOwnerFilter.ALL)) },
                label = { Text(stringResource(Res.string.filter_all_orders)) },
            )
            FilterChip(
                selected = filter.owner == OrderOwnerFilter.MINE,
                onClick = { onIntent(CurrentOrdersIntent.SelectOwner(OrderOwnerFilter.MINE)) },
                label = { Text(stringResource(Res.string.filter_my_orders)) },
            )
        }
        FilterRow {
            FilterChip(
                selected = filter.state == null,
                onClick = { onIntent(CurrentOrdersIntent.SelectState(null)) },
                label = { Text(stringResource(Res.string.filter_all_states)) },
            )
            listOf(OrderState.IN_PROGRESS, OrderState.CLOSED).forEach { state ->
                FilterChip(
                    selected = filter.state == state,
                    onClick = { onIntent(CurrentOrdersIntent.SelectState(state)) },
                    label = { Text(stringResource(state.label())) },
                )
            }
        }
    }
}

@Composable
private fun FilterRow(chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { chips() }
}

@Composable
private fun OrderRow(listing: OrderListing, onClick: () -> Unit) {
    val order = listing.order
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = order.destination.label(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = listOfNotNull(
                    employeeNameOrUnknown(listing.employeeName),
                    order.createdAt?.let(::formatBogotaDateTime),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderStateBadge(order.state)
                if (order.isPendingToSend) PendingToSendBadge()
            }
        }
        Text(
            text = formatPesos(order.totalAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
