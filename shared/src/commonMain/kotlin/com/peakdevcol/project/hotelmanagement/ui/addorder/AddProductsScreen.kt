package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.add_products_action
import com.peakdevcol.project.hotelmanagement.resources.add_products_count
import com.peakdevcol.project.hotelmanagement.resources.add_products_title
import com.peakdevcol.project.hotelmanagement.resources.order_add_products
import com.peakdevcol.project.hotelmanagement.resources.order_not_in_progress
import com.peakdevcol.project.hotelmanagement.resources.products_added
import com.peakdevcol.project.hotelmanagement.resources.products_added_offline
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import com.peakdevcol.project.hotelmanagement.ui.currentorders.message
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OrderSummaryBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** RF-CUR-06 and RF-CUR-13: products for an order in progress, with the same grid as a new order. */
@Composable
fun AddProductsScreen(
    orderId: String,
    onClose: () -> Unit,
    viewModel: AddProductsViewModel = koinViewModel(key = "add-products-$orderId") { parametersOf(orderId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddProductsEffect.Added -> {
                    messages.show(getString(if (effect.delivery == OrderDelivery.SENT) Res.string.products_added else Res.string.products_added_offline))
                    onClose()
                }
                is AddProductsEffect.Failed -> messages.show(effect.error.message())
            }
        }
    }

    AddProductsContent(state = state, onIntent = viewModel::onIntent, onClose = onClose)
}

@Composable
fun AddProductsContent(state: AddProductsState, onIntent: (AddProductsIntent) -> Unit, onClose: () -> Unit) {
    val order = state.order
    val selection = state.picker.selection
    Scaffold(
        topBar = {
            MajesticTopBar(
                title = if (order != null) stringResource(Res.string.add_products_title, order.destination.label()) else stringResource(Res.string.order_add_products),
                onBack = onClose,
            )
        },
        bottomBar = {
            OrderSummaryBar(
                detail = pluralStringResource(Res.plurals.add_products_count, selection.unitCount, selection.unitCount),
                totalAmount = selection.totalAmount,
                actionText = stringResource(Res.string.add_products_action),
                onAction = { onIntent(AddProductsIntent.Confirm) },
                actionEnabled = state.canConfirm,
                actionLoading = state.isSaving,
            )
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        ProductPickerContent(
            picker = state.picker,
            onIntent = { onIntent(AddProductsIntent.Picker(it)) },
            onRetryCatalog = { onIntent(AddProductsIntent.RetryCatalog) },
            modifier = Modifier.padding(padding),
            header = if (order != null && !state.isOrderInProgress) {
                { Banner(tone = BannerTone.Warning, icon = MajesticIcons.warning, message = stringResource(Res.string.order_not_in_progress)) }
            } else {
                null
            },
        )
    }
}
