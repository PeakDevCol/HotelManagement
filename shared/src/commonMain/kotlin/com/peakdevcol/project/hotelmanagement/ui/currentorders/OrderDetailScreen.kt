package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.OrderAction
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDetail
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEventType
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.order_actions_offline
import com.peakdevcol.project.hotelmanagement.resources.order_cancel_action
import com.peakdevcol.project.hotelmanagement.resources.order_cancelled_done
import com.peakdevcol.project.hotelmanagement.resources.order_close_action
import com.peakdevcol.project.hotelmanagement.resources.order_close_message
import com.peakdevcol.project.hotelmanagement.resources.order_closed_done
import com.peakdevcol.project.hotelmanagement.resources.order_detail_error
import com.peakdevcol.project.hotelmanagement.resources.order_detail_history
import com.peakdevcol.project.hotelmanagement.resources.order_detail_not_found
import com.peakdevcol.project.hotelmanagement.resources.order_detail_products
import com.peakdevcol.project.hotelmanagement.resources.order_detail_taken_by
import com.peakdevcol.project.hotelmanagement.resources.order_detail_title
import com.peakdevcol.project.hotelmanagement.resources.order_error_invalid_tip
import com.peakdevcol.project.hotelmanagement.resources.order_error_missing_reason
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_connection
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.order_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.order_error_state_changed
import com.peakdevcol.project.hotelmanagement.resources.order_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.order_event_cancel_reason
import com.peakdevcol.project.hotelmanagement.resources.order_event_cancelled
import com.peakdevcol.project.hotelmanagement.resources.order_event_closed
import com.peakdevcol.project.hotelmanagement.resources.order_event_created
import com.peakdevcol.project.hotelmanagement.resources.order_event_line
import com.peakdevcol.project.hotelmanagement.resources.order_event_paid
import com.peakdevcol.project.hotelmanagement.resources.order_event_payment
import com.peakdevcol.project.hotelmanagement.resources.order_event_reopened
import com.peakdevcol.project.hotelmanagement.resources.order_paid_done
import com.peakdevcol.project.hotelmanagement.resources.order_pay_action
import com.peakdevcol.project.hotelmanagement.resources.order_product_line
import com.peakdevcol.project.hotelmanagement.resources.order_reopen_action
import com.peakdevcol.project.hotelmanagement.resources.order_reopen_message
import com.peakdevcol.project.hotelmanagement.resources.order_reopened_done
import com.peakdevcol.project.hotelmanagement.resources.order_total
import com.peakdevcol.project.hotelmanagement.resources.order_unit_price
import com.peakdevcol.project.hotelmanagement.ui.addorder.label
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> snackbarHostState.showSnackbar(effect.message()) }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.order_detail_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            (state as? OrderDetailState.Loaded)?.let { loaded ->
                OrderActions(state = loaded, onAction = { viewModel.onIntent(OrderDetailIntent.ActionClick(it)) })
            }
        },
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

    (state as? OrderDetailState.Loaded)?.let { loaded ->
        OrderActionDialog(state = loaded, onIntent = viewModel::onIntent)
        if (loaded.isWorking) LoadingOverlay()
    }
}

@Composable
private fun OrderActionDialog(state: OrderDetailState.Loaded, onIntent: (OrderDetailIntent) -> Unit) {
    val dismiss = { onIntent(OrderDetailIntent.DismissDialog) }
    when (state.dialog) {
        OrderAction.CLOSE -> ConfirmActionDialog(
            title = stringResource(Res.string.order_close_action),
            message = stringResource(Res.string.order_close_message),
            onConfirm = { onIntent(OrderDetailIntent.ConfirmClose) },
            onDismiss = dismiss,
        )
        OrderAction.REOPEN -> ConfirmActionDialog(
            title = stringResource(Res.string.order_reopen_action),
            message = stringResource(Res.string.order_reopen_message),
            onConfirm = { onIntent(OrderDetailIntent.ConfirmReopen) },
            onDismiss = dismiss,
        )
        OrderAction.PAY -> PaymentDialog(
            orderAmount = state.detail.order.totalAmount,
            onSubmit = { method, tipAmount -> onIntent(OrderDetailIntent.SubmitPayment(method, tipAmount)) },
            onDismiss = dismiss,
        )
        OrderAction.CANCEL -> CancelOrderDialog(
            onSubmit = { reason -> onIntent(OrderDetailIntent.SubmitCancellation(reason)) },
            onDismiss = dismiss,
        )
        null -> Unit
    }
}

@Composable
private fun OrderActions(state: OrderDetailState.Loaded, onAction: (OrderAction) -> Unit) {
    val actions = state.detail.availableActions
    if (actions.isEmpty()) return
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isOffline) {
                Text(
                    text = stringResource(Res.string.order_actions_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            actions.filterNot { it == OrderAction.CANCEL }.forEach { action ->
                PrimaryButton(
                    text = stringResource(action.label()),
                    onClick = { onAction(action) },
                    enabled = state.canChangeState,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            }
            if (OrderAction.CANCEL in actions) {
                TextButton(onClick = { onAction(OrderAction.CANCEL) }, enabled = state.canChangeState) {
                    Text(stringResource(OrderAction.CANCEL.label()), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun OrderAction.label(): StringResource = when (this) {
    OrderAction.CLOSE -> Res.string.order_close_action
    OrderAction.REOPEN -> Res.string.order_reopen_action
    OrderAction.PAY -> Res.string.order_pay_action
    OrderAction.CANCEL -> Res.string.order_cancel_action
}

private suspend fun OrderDetailEffect.message(): String = when (this) {
    is OrderDetailEffect.ActionDone -> getString(
        when (action) {
            OrderAction.CLOSE -> Res.string.order_closed_done
            OrderAction.REOPEN -> Res.string.order_reopened_done
            OrderAction.PAY -> Res.string.order_paid_done
            OrderAction.CANCEL -> Res.string.order_cancelled_done
        },
    )
    is OrderDetailEffect.ActionFailed -> when (error) {
        is OrderActionError.StateChanged ->
            getString(Res.string.order_error_state_changed, getString(error.currentState.label()))
        OrderActionError.NoConnection -> getString(Res.string.order_error_no_connection)
        OrderActionError.NotAllowed -> getString(Res.string.order_error_not_allowed)
        OrderActionError.NotFound -> getString(Res.string.order_detail_not_found)
        OrderActionError.MissingReason -> getString(Res.string.order_error_missing_reason)
        OrderActionError.InvalidTip -> getString(Res.string.order_error_invalid_tip)
        OrderActionError.NoSession -> getString(Res.string.order_error_no_session)
        OrderActionError.Unknown -> getString(Res.string.order_error_unknown)
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
    val action = stringResource(
        when (event.type) {
            OrderEventType.CREATED -> Res.string.order_event_created
            OrderEventType.CLOSED -> Res.string.order_event_closed
            OrderEventType.REOPENED -> Res.string.order_event_reopened
            OrderEventType.PAID -> Res.string.order_event_paid
            OrderEventType.CANCELLED -> Res.string.order_event_cancelled
        },
    )
    Column {
        Text(
            text = stringResource(Res.string.order_event_line, action, employeeNameOrUnknown(event.employeeName)),
            style = MaterialTheme.typography.bodyLarge,
        )
        event.paymentMethod?.let { method ->
            Text(
                text = stringResource(
                    Res.string.order_event_payment,
                    stringResource(method.label()),
                    formatPesos(event.tipAmount ?: 0),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        event.cancelReason?.let { reason ->
            Text(
                text = stringResource(Res.string.order_event_cancel_reason, reason),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        event.at?.let { at ->
            Text(
                text = formatBogotaDateTime(at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
