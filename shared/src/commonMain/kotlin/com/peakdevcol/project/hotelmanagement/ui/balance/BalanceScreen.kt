package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceOption
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatTimeOfDay
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_change_period
import com.peakdevcol.project.hotelmanagement.resources.balance_error_no_connection
import com.peakdevcol.project.hotelmanagement.resources.balance_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.balance_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_all
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_destination
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_employee
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_method
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_product
import com.peakdevcol.project.hotelmanagement.resources.balance_period
import com.peakdevcol.project.hotelmanagement.resources.balance_period_hint
import com.peakdevcol.project.hotelmanagement.resources.balance_period_invalid
import com.peakdevcol.project.hotelmanagement.resources.balance_period_too_long
import com.peakdevcol.project.hotelmanagement.resources.balance_reload
import com.peakdevcol.project.hotelmanagement.resources.destination_bar
import com.peakdevcol.project.hotelmanagement.resources.destination_other
import com.peakdevcol.project.hotelmanagement.resources.destination_pool
import com.peakdevcol.project.hotelmanagement.resources.destination_room
import com.peakdevcol.project.hotelmanagement.resources.destination_table
import com.peakdevcol.project.hotelmanagement.resources.menu_balance
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-BAL-01 to RF-BAL-04 and RF-BAL-07: only administrators open it (PRD, section 3). */
@Composable
fun BalanceScreen(onBack: () -> Unit, viewModel: BalanceViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> snackbarHostState.showSnackbar(effect.message()) }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.menu_balance), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val loadError = state.loadError
        when {
            state.report == null && loadError != null -> CenteredMessage(
                message = stringResource(loadError.messageRes()),
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.onIntent(BalanceIntent.Reload) },
            )
            state.report == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> BalanceContent(state = state, onIntent = viewModel::onIntent, modifier = Modifier.padding(padding))
        }
    }

    val period = state.period
    if (state.isPickingPeriod && period != null) {
        BalancePeriodDialog(
            current = period,
            onConfirm = { first, last -> viewModel.onIntent(BalanceIntent.ChangePeriod(first, last)) },
            onDismiss = { viewModel.onIntent(BalanceIntent.DismissPeriodPicker) },
        )
    }
}

@Composable
private fun BalanceContent(state: BalanceState, onIntent: (BalanceIntent) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.loadError?.let { error ->
                Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PeriodHeader(state = state, onIntent = onIntent)
            Filters(state = state, onIntent = onIntent)
            state.summary?.let { summary ->
                TotalsCard(totals = summary.totals, productSales = summary.byProduct.singleOrNull()?.takeIf { state.filter.productId != null })
            }
            state.pending?.let { PendingCard(it) }
            state.summary?.let { summary ->
                Breakdowns(summary = summary, selected = state.breakdown, onSelect = { onIntent(BalanceIntent.ShowBreakdown(it)) })
            }
        }
    }
}

@Composable
private fun PeriodHeader(state: BalanceState, onIntent: (BalanceIntent) -> Unit) {
    val report = state.report ?: return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.balance_period, formatCalendarDate(report.period.firstDay), formatCalendarDate(report.period.lastDay)),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.balance_period_hint, formatTimeOfDay(report.dayStartMinutes)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onIntent(BalanceIntent.OpenPeriodPicker) }, enabled = !state.isLoading) {
                Text(stringResource(Res.string.balance_change_period))
            }
            TextButton(onClick = { onIntent(BalanceIntent.Reload) }, enabled = !state.isLoading) {
                Text(stringResource(Res.string.balance_reload))
            }
        }
    }
}

/** RF-BAL-02: the filters work on the payments already read. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(state: BalanceState, onIntent: (BalanceIntent) -> Unit) {
    val filter = state.filter
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterMenu(
            title = stringResource(Res.string.balance_filter_method),
            selected = filter.method,
            options = PaymentMethod.entries.map { it to stringResource(it.label()) },
            onSelect = { onIntent(BalanceIntent.FilterByMethod(it)) },
        )
        FilterMenu(
            title = stringResource(Res.string.balance_filter_employee),
            selected = filter.employeeId,
            options = state.employeeOptions.map { it.id to employeeNameOrUnknown(it.name) },
            onSelect = { onIntent(BalanceIntent.FilterByEmployee(it)) },
        )
        FilterMenu(
            title = stringResource(Res.string.balance_filter_destination),
            selected = filter.destination,
            options = DestinationKind.entries.map { it to stringResource(it.label()) },
            onSelect = { onIntent(BalanceIntent.FilterByDestination(it)) },
        )
        FilterMenu(
            title = stringResource(Res.string.balance_filter_product),
            selected = filter.productId,
            options = state.productOptions.map(BalanceOption::productEntry),
            onSelect = { onIntent(BalanceIntent.FilterByProduct(it)) },
        )
    }
}

private fun BalanceOption.productEntry(): Pair<String, String> = id to (name ?: id)

/** A chip that opens the values of one filter. The first entry, "Todos", removes it. */
@Composable
private fun <T> FilterMenu(title: String, selected: T?, options: List<Pair<T, String>>, onSelect: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selected }?.second
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(if (selectedName != null) "$title: $selectedName" else title) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.balance_filter_all)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

fun DestinationKind.label(): StringResource = when (this) {
    DestinationKind.ROOM -> Res.string.destination_room
    DestinationKind.TABLE -> Res.string.destination_table
    DestinationKind.POOL -> Res.string.destination_pool
    DestinationKind.BAR -> Res.string.destination_bar
    DestinationKind.OTHER -> Res.string.destination_other
}

private fun PaymentReadError.messageRes(): StringResource = when (this) {
    PaymentReadError.NoConnection -> Res.string.balance_error_no_connection
    PaymentReadError.NotAllowed -> Res.string.balance_error_not_allowed
    PaymentReadError.Unknown -> Res.string.balance_error_unknown
}

private suspend fun BalanceEffect.message(): String = when (this) {
    is BalanceEffect.PeriodRejected -> getString(
        when (error) {
            BalanceError.InvalidPeriod -> Res.string.balance_period_invalid
            BalanceError.PeriodTooLong -> Res.string.balance_period_too_long
        },
    )
}
