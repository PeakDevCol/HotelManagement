package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.order_detail_error
import com.peakdevcol.project.hotelmanagement.resources.order_detail_history
import com.peakdevcol.project.hotelmanagement.resources.order_detail_not_found
import com.peakdevcol.project.hotelmanagement.resources.order_detail_products
import com.peakdevcol.project.hotelmanagement.resources.order_detail_taken_by
import com.peakdevcol.project.hotelmanagement.resources.order_detail_title
import com.peakdevcol.project.hotelmanagement.resources.order_event_created
import com.peakdevcol.project.hotelmanagement.resources.order_event_line
import com.peakdevcol.project.hotelmanagement.resources.order_product_line
import com.peakdevcol.project.hotelmanagement.resources.order_total
import com.peakdevcol.project.hotelmanagement.resources.order_unit_price
import com.peakdevcol.project.hotelmanagement.ui.addorder.label
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = koinViewModel(key = orderId) { parametersOf(orderId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.order_detail_title), onBack = onBack) },
    ) { padding ->
        when (val current = state) {
            OrderDetailState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            OrderDetailState.NotFound -> CenteredMessage(
                message = stringResource(Res.string.order_detail_not_found),
                modifier = Modifier.padding(padding),
            )
            OrderDetailState.LoadFailed -> CenteredMessage(
                message = stringResource(Res.string.order_detail_error),
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.onIntent(OrderDetailIntent.Retry) },
            )
            is OrderDetailState.Loaded -> OrderDetailContent(detail = current.detail, padding = padding)
        }
    }
}

@Composable
private fun OrderDetailContent(detail: OrderDetail, padding: PaddingValues) {
    val order = detail.order
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = order.destination.label(), style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderStateBadge(order.state)
                    if (order.isPendingToSend) PendingToSendBadge()
                }
                Text(
                    text = stringResource(Res.string.order_detail_taken_by, employeeNameOrUnknown(detail.employeeName)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                order.createdAt?.let { createdAt ->
                    Text(
                        text = formatBogotaDateTime(createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { SectionTitle(stringResource(Res.string.order_detail_products)) }
        items(order.products, key = { it.id }) { product -> ProductLine(product) }
        item {
            HorizontalDivider()
            Text(
                text = stringResource(Res.string.order_total, formatPesos(order.totalAmount)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item { SectionTitle(stringResource(Res.string.order_detail_history)) }
        items(detail.history) { event -> HistoryLine(event) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun ProductLine(product: Product) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
        Text(text = formatPesos(product.totalAmount), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HistoryLine(event: OrderEvent) {
    val action = when (event.type) {
        OrderEventType.CREATED -> stringResource(Res.string.order_event_created)
    }
    Column {
        Text(
            text = stringResource(Res.string.order_event_line, action, employeeNameOrUnknown(event.employeeName)),
            style = MaterialTheme.typography.bodyLarge,
        )
        event.at?.let { at ->
            Text(
                text = formatBogotaDateTime(at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
