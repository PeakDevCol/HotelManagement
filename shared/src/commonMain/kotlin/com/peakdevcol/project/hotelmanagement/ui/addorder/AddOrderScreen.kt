package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.destination_change
import com.peakdevcol.project.hotelmanagement.resources.destination_choose
import com.peakdevcol.project.hotelmanagement.resources.destination_label
import com.peakdevcol.project.hotelmanagement.resources.order_choose_destination
import com.peakdevcol.project.hotelmanagement.resources.order_confirm
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.order_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.order_saved_on_device
import com.peakdevcol.project.hotelmanagement.resources.order_sent
import com.peakdevcol.project.hotelmanagement.resources.order_without_destination
import com.peakdevcol.project.hotelmanagement.resources.orders_new
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import com.peakdevcol.project.hotelmanagement.ui.currentorders.productCountLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OrderSummaryBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.icon
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-ORD-01 to RF-ORD-04: a new order. When it is confirmed the list of orders shows it at once. */
@Composable
fun AddOrderScreen(onClose: () -> Unit, viewModel: AddOrderViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddOrderEffect.Saved -> {
                    messages.show(getString(if (effect.delivery == OrderDelivery.SENT) Res.string.order_sent else Res.string.order_saved_on_device))
                    onClose()
                }
                is AddOrderEffect.SaveFailed -> messages.show(
                    getString(
                        when (effect.reason) {
                            SaveFailure.NoSession -> Res.string.order_error_no_session
                            SaveFailure.Unknown -> Res.string.order_error_unknown
                        },
                    ),
                )
            }
        }
    }

    AddOrderContent(state = state, onIntent = viewModel::onIntent, onClose = onClose)
}

@Composable
fun AddOrderContent(state: AddOrderState, onIntent: (AddOrderIntent) -> Unit, onClose: () -> Unit) {
    val destination = state.destination
    val selection = state.picker.selection
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.orders_new), onBack = onClose) },
        bottomBar = {
            OrderSummaryBar(
                detail = listOf(
                    if (destination == null) stringResource(Res.string.order_without_destination) else destination.label(),
                    productCountLabel(selection.unitCount),
                ).joinToString(" · "),
                totalAmount = selection.totalAmount,
                actionText = stringResource(if (destination == null) Res.string.order_choose_destination else Res.string.order_confirm),
                onAction = { onIntent(AddOrderIntent.Confirm) },
                actionEnabled = destination == null || state.canConfirm,
                actionLoading = state.isSaving,
            )
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        ProductPickerContent(
            picker = state.picker,
            onIntent = { onIntent(AddOrderIntent.Picker(it)) },
            onRetryCatalog = { onIntent(AddOrderIntent.RetryCatalog) },
            modifier = Modifier.padding(padding),
            header = { DestinationCard(destination = destination, onClick = { onIntent(AddOrderIntent.OpenDestination) }) },
        )
    }
    state.destinationDraft?.let { draft -> DestinationSheet(draft = draft, onIntent = onIntent) }
}

/** RF-ORD-03: the destination goes first, and changes with one touch. */
@Composable
private fun DestinationCard(destination: OrderDestination?, onClick: () -> Unit) {
    val colors = MajesticTheme.colors
    ListGroup {
        ListRow(
            title = if (destination == null) stringResource(Res.string.destination_choose) else destination.label(),
            subtitle = stringResource(Res.string.destination_label),
            leading = { Avatar(icon = destination.icon(filled = destination != null)) },
            trailing = {
                Text(
                    text = stringResource(Res.string.destination_change),
                    style = MajesticTheme.typography.label,
                    color = colors.primary,
                    maxLines = 1,
                )
            },
            onClick = onClick,
        )
    }
}
