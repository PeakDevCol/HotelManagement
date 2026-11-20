package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_clear_filters
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_all
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_destination
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_employee
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_method
import com.peakdevcol.project.hotelmanagement.resources.balance_filter_product
import com.peakdevcol.project.hotelmanagement.resources.balance_filters
import com.peakdevcol.project.hotelmanagement.resources.done
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.stringResource

/** RF-BAL-02: the filters work on the payments already read, so changing them costs no reads (RNF-12). */
@Composable
fun BalanceFiltersSheet(state: BalanceState, onIntent: (BalanceIntent) -> Unit) {
    val filter = state.filter
    TaskSheet(
        title = stringResource(Res.string.balance_filters),
        onDismiss = { onIntent(BalanceIntent.DismissFilters) },
        asDialog = LocalWideWindow.current,
    ) {
        FilterGroup(
            title = stringResource(Res.string.balance_filter_method),
            selected = filter.method,
            options = PaymentMethod.entries.map { it to stringResource(it.label()) },
            onSelect = { onIntent(BalanceIntent.FilterByMethod(it)) },
        )
        FilterGroup(
            title = stringResource(Res.string.balance_filter_employee),
            selected = filter.employeeId,
            options = state.employeeOptions.map { it.id to employeeNameOrUnknown(it.name) },
            onSelect = { onIntent(BalanceIntent.FilterByEmployee(it)) },
        )
        FilterGroup(
            title = stringResource(Res.string.balance_filter_destination),
            selected = filter.destination,
            options = DestinationKind.entries.map { it to stringResource(it.label()) },
            onSelect = { onIntent(BalanceIntent.FilterByDestination(it)) },
        )
        FilterGroup(
            title = stringResource(Res.string.balance_filter_product),
            selected = filter.productId,
            options = state.productOptions.map { it.id to (it.name ?: it.id) },
            onSelect = { onIntent(BalanceIntent.FilterByProduct(it)) },
        )
        MajesticButton(
            text = stringResource(Res.string.done),
            onClick = { onIntent(BalanceIntent.DismissFilters) },
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.activeFilterCount > 0) {
            MajesticButton(
                text = stringResource(Res.string.balance_clear_filters),
                onClick = { onIntent(BalanceIntent.ClearFilters) },
                style = ButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** One filter: "Todos" removes it, and each option keeps only its payments. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FilterGroup(title: String, selected: T?, options: List<Pair<T, String>>, onSelect: (T?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = title)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
        ) {
            MajesticChip(text = stringResource(Res.string.balance_filter_all), selected = selected == null, onClick = { onSelect(null) })
            options.forEach { (value, name) ->
                MajesticChip(text = name, selected = selected == value, onClick = { onSelect(value) })
            }
        }
    }
}
