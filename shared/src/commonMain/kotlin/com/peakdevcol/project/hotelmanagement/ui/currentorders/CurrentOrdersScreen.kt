package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.OrderOwnerFilter
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.current_orders_error
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.filter_all
import com.peakdevcol.project.hotelmanagement.resources.filter_closed
import com.peakdevcol.project.hotelmanagement.resources.filter_in_progress
import com.peakdevcol.project.hotelmanagement.resources.filter_mine
import com.peakdevcol.project.hotelmanagement.resources.nav_orders
import com.peakdevcol.project.hotelmanagement.resources.orders_empty_filter_message
import com.peakdevcol.project.hotelmanagement.resources.orders_empty_filter_title
import com.peakdevcol.project.hotelmanagement.resources.orders_empty_message
import com.peakdevcol.project.hotelmanagement.resources.orders_empty_title
import com.peakdevcol.project.hotelmanagement.resources.orders_new
import com.peakdevcol.project.hotelmanagement.resources.orders_pending_message
import com.peakdevcol.project.hotelmanagement.resources.orders_pending_title
import com.peakdevcol.project.hotelmanagement.resources.orders_search
import com.peakdevcol.project.hotelmanagement.resources.orders_search_close
import com.peakdevcol.project.hotelmanagement.resources.orders_search_placeholder
import com.peakdevcol.project.hotelmanagement.resources.orders_select_message
import com.peakdevcol.project.hotelmanagement.resources.orders_select_title
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDetailLayout
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDetailMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OrderCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ScrollingChipRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SearchTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.icon
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalShowsMessages
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-CUR-01 and RF-CUR-02: where the app opens for most areas. */
@Composable
fun CurrentOrdersScreen(
    onNewOrder: () -> Unit,
    onOpenOrder: (orderId: String) -> Unit,
    onAddProducts: (orderId: String) -> Unit,
    viewModel: CurrentOrdersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CurrentOrdersEffect.NavigateToOrder -> onOpenOrder(effect.orderId)
                CurrentOrdersEffect.NavigateToNewOrder -> onNewOrder()
            }
        }
    }

    CurrentOrdersContent(
        state = state,
        onIntent = viewModel::onIntent,
        orderDetail = { orderId -> OrderDetailScreen(orderId = orderId, onBack = null, onAddProducts = { onAddProducts(orderId) }) },
    )
}

/** On wide windows the chosen order opens beside the list, drawn by [orderDetail]. */
@Composable
fun CurrentOrdersContent(
    state: CurrentOrdersState,
    onIntent: (CurrentOrdersIntent) -> Unit,
    orderDetail: @Composable (orderId: String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val besideList = maxWidth >= ListDetailMinWidth
        Scaffold(
            topBar = { OrdersTopBar(state = state, onIntent = onIntent, showsNewOrderButton = besideList) },
            floatingActionButton = {
                if (!besideList) {
                    MajesticFloatingActionButton(
                        text = stringResource(Res.string.orders_new),
                        icon = MajesticIcons.add,
                        onClick = { onIntent(CurrentOrdersIntent.NewOrder) },
                    )
                }
            },
            snackbarHost = { AppMessagesHost() },
            containerColor = MajesticTheme.colors.surface,
        ) { padding ->
            if (besideList) {
                ListDetailLayout(
                    modifier = Modifier.padding(padding),
                    list = { OrdersList(state = state, onIntent = onIntent, besideList = true) },
                    detail = { SelectedOrder(orderId = state.selectedOrderId, orderDetail = orderDetail) },
                )
            } else {
                OrdersList(state = state, onIntent = onIntent, besideList = false, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun OrdersTopBar(state: CurrentOrdersState, onIntent: (CurrentOrdersIntent) -> Unit, showsNewOrderButton: Boolean) {
    if (state.isSearchOpen) {
        SearchTopBar(
            query = state.filter.query,
            onQueryChange = { onIntent(CurrentOrdersIntent.Search(it)) },
            placeholder = stringResource(Res.string.orders_search_placeholder),
            closeDescription = stringResource(Res.string.orders_search_close),
            onClose = { onIntent(CurrentOrdersIntent.CloseSearch) },
        )
        return
    }
    MajesticTopBar(title = stringResource(Res.string.nav_orders)) {
        MajesticIconButton(
            icon = MajesticIcons.search,
            contentDescription = stringResource(Res.string.orders_search),
            onClick = { onIntent(CurrentOrdersIntent.OpenSearch) },
            tint = MajesticTheme.colors.ink,
        )
        if (showsNewOrderButton) {
            MajesticButton(
                text = stringResource(Res.string.orders_new),
                onClick = { onIntent(CurrentOrdersIntent.NewOrder) },
                icon = MajesticIcons.add,
                modifier = Modifier.padding(start = MajesticSpacing.space2, end = MajesticSpacing.space3),
            )
        }
    }
}

@Composable
private fun OrdersList(state: CurrentOrdersState, onIntent: (CurrentOrdersIntent) -> Unit, besideList: Boolean, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> MajesticLoading(modifier)
        state.loadFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.current_orders_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(CurrentOrdersIntent.Retry) },
            modifier = modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MajesticSpacing.space4,
                end = MajesticSpacing.space4,
                top = MajesticSpacing.space2,
                // Room for the floating button, so it never covers the last order.
                bottom = if (besideList) MajesticSpacing.space4 else MajesticSizes.floatingActionButtonClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        ) {
            if (state.pendingToSendCount > 0) {
                item(key = "notice-pending") {
                    Banner(
                        tone = BannerTone.Warning,
                        icon = MajesticIcons.cloudUpload,
                        title = pluralStringResource(Res.plurals.orders_pending_title, state.pendingToSendCount, state.pendingToSendCount),
                        message = stringResource(Res.string.orders_pending_message),
                    )
                }
            }
            item(key = "notice-filters") { OrderFilters(state = state, onIntent = onIntent) }
            ordersOrEmpty(state = state, onIntent = onIntent, besideList = besideList)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ordersOrEmpty(
    state: CurrentOrdersState,
    onIntent: (CurrentOrdersIntent) -> Unit,
    besideList: Boolean,
) {
    val visible = state.visibleOrders
    when {
        state.orders.isEmpty() -> item(key = "notice-empty") {
            EmptyState(
                icon = MajesticIcons.receipt,
                title = stringResource(Res.string.orders_empty_title),
                message = stringResource(Res.string.orders_empty_message),
                actionText = stringResource(Res.string.orders_new),
                actionIcon = MajesticIcons.add,
                onAction = { onIntent(CurrentOrdersIntent.NewOrder) },
            )
        }
        visible.isEmpty() -> item(key = "notice-empty-filter") {
            EmptyState(
                icon = MajesticIcons.filter,
                title = stringResource(Res.string.orders_empty_filter_title),
                message = stringResource(Res.string.orders_empty_filter_message),
            )
        }
        // Prefixed, so an order id never takes the key of a notice.
        else -> items(visible, key = { "order-${it.order.id}" }) { listing ->
            val order = listing.order
            OrderCard(
                destination = order.destination.label(),
                destinationIcon = order.destination.icon(),
                state = order.state,
                meta = listing.summary(),
                age = elapsedLabel(order.createdAt, state.now),
                totalAmount = order.totalAmount,
                isPendingToSend = order.isPendingToSend,
                selected = besideList && order.id == state.selectedOrderId,
                onClick = { onIntent(CurrentOrdersIntent.OpenOrder(order.id, besideList)) },
            )
        }
    }
}

/** RF-CUR-02: whose orders and in what state. Each chip says how many orders it would show. */
@Composable
private fun OrderFilters(state: CurrentOrdersState, onIntent: (CurrentOrdersIntent) -> Unit) {
    val filter = state.filter
    ScrollingChipRow {
        MajesticChip(
            text = stringResource(Res.string.filter_all),
            selected = filter.owner == OrderOwnerFilter.ALL,
            count = state.countWith(filter.copy(owner = OrderOwnerFilter.ALL)),
            onClick = { onIntent(CurrentOrdersIntent.SelectOwner(OrderOwnerFilter.ALL)) },
        )
        MajesticChip(
            text = stringResource(Res.string.filter_mine),
            selected = filter.owner == OrderOwnerFilter.MINE,
            count = state.countWith(filter.copy(owner = OrderOwnerFilter.MINE)),
            onClick = { onIntent(CurrentOrdersIntent.SelectOwner(OrderOwnerFilter.MINE)) },
        )
        listOf(OrderState.IN_PROGRESS to Res.string.filter_in_progress, OrderState.CLOSED to Res.string.filter_closed).forEach { (orderState, label) ->
            MajesticChip(
                text = stringResource(label),
                selected = filter.state == orderState,
                count = state.countWith(filter.copy(state = orderState)),
                onClick = { onIntent(CurrentOrdersIntent.ToggleState(orderState)) },
            )
        }
    }
}

@Composable
private fun SelectedOrder(orderId: String?, orderDetail: @Composable (orderId: String) -> Unit) {
    if (orderId == null) {
        EmptyState(
            icon = MajesticIcons.receipt,
            title = stringResource(Res.string.orders_select_title),
            message = stringResource(Res.string.orders_select_message),
        )
        return
    }
    // The list already shows the messages; the detail beside it does not repeat them.
    CompositionLocalProvider(LocalShowsMessages provides false) {
        key(orderId) { orderDetail(orderId) }
    }
}
