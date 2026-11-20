package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceSummary
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceTotals
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_by_day
import com.peakdevcol.project.hotelmanagement.resources.balance_by_employee
import com.peakdevcol.project.hotelmanagement.resources.balance_by_method
import com.peakdevcol.project.hotelmanagement.resources.balance_by_product
import com.peakdevcol.project.hotelmanagement.resources.balance_day_without_closing_line
import com.peakdevcol.project.hotelmanagement.resources.balance_empty
import com.peakdevcol.project.hotelmanagement.resources.balance_open_orders
import com.peakdevcol.project.hotelmanagement.resources.balance_payments
import com.peakdevcol.project.hotelmanagement.resources.balance_pending_title
import com.peakdevcol.project.hotelmanagement.resources.balance_product_detail
import com.peakdevcol.project.hotelmanagement.resources.balance_product_line
import com.peakdevcol.project.hotelmanagement.resources.balance_product_note
import com.peakdevcol.project.hotelmanagement.resources.balance_product_sold
import com.peakdevcol.project.hotelmanagement.resources.balance_tips
import com.peakdevcol.project.hotelmanagement.resources.balance_tips_detail
import com.peakdevcol.project.hotelmanagement.resources.balance_total_received
import com.peakdevcol.project.hotelmanagement.resources.balance_totals_detail
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountText
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTabs
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatEmphasis
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileData
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.StatTileRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * RF-BAL-01, RF-BAL-04 and RF-BAL-07: what came in, the tips apart and what is still to be
 * charged. With a product chosen, the first figure is what that product sold, without tips.
 */
@Composable
fun BalanceTiles(state: BalanceState) {
    val summary = state.summary ?: return
    val totals = summary.totals
    val productSales = summary.byProduct.singleOrNull()?.takeIf { state.filter.productId != null }
    val tiles = buildList {
        if (productSales == null) {
            add(
                StatTileData(
                    label = stringResource(Res.string.balance_total_received),
                    value = formatPesos(totals.totalReceived),
                    detail = stringResource(
                        Res.string.balance_totals_detail,
                        pluralStringResource(Res.plurals.balance_payments, totals.paymentCount, totals.paymentCount),
                        formatPesos(totals.salesWithoutTax),
                        formatPesos(totals.taxAmount),
                    ),
                    emphasis = StatEmphasis.Brand,
                ),
            )
            add(StatTileData(label = stringResource(Res.string.balance_tips), value = formatPesos(totals.tipAmount), detail = stringResource(Res.string.balance_tips_detail)))
        } else {
            add(
                StatTileData(
                    label = stringResource(Res.string.balance_product_sold, productSales.productName),
                    value = formatPesos(totals.salesAmount),
                    detail = stringResource(Res.string.balance_product_detail, productSales.quantity, formatPesos(totals.salesWithoutTax), formatPesos(totals.taxAmount)),
                    emphasis = StatEmphasis.Brand,
                ),
            )
        }
        state.pending?.let { pending ->
            val openOrders = pending.inProgressCount + pending.closedCount
            add(
                StatTileData(
                    label = stringResource(Res.string.balance_pending_title),
                    value = formatPesos(pending.totalAmount),
                    detail = pluralStringResource(Res.plurals.balance_open_orders, openOrders, openOrders),
                ),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        StatTileRow(tiles = tiles)
        if (productSales != null) {
            Text(text = stringResource(Res.string.balance_product_note), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
        }
    }
}

/** RF-BAL-03: the totals by day, method, employee or product, after the filters. */
@Composable
fun Breakdowns(
    summary: BalanceSummary,
    selected: BalanceBreakdown,
    daysWithoutDailyClosing: Set<CalendarDate>,
    onSelect: (BalanceBreakdown) -> Unit,
) {
    SectionCard {
        MajesticTabs(
            tabs = BalanceBreakdown.entries.map { stringResource(it.title()) },
            selectedIndex = selected.ordinal,
            onSelect = { onSelect(BalanceBreakdown.entries[it]) },
        )
        if (summary.totals.paymentCount == 0) {
            Text(text = stringResource(Res.string.balance_empty), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            return@SectionCard
        }
        Column(verticalArrangement = Arrangement.Top) {
            breakdownRows(summary, selected, daysWithoutDailyClosing).forEachIndexed { index, row ->
                if (index > 0) ListDivider()
                ListRow(title = row.title, subtitle = row.detail, trailing = { AmountText(amount = row.amount, style = MajesticTheme.typography.label.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize, fontFeatureSettings = "tnum")) })
            }
        }
    }
}

private data class BreakdownRow(val title: String, val detail: String, val amount: Long)

@Composable
private fun breakdownRows(summary: BalanceSummary, selected: BalanceBreakdown, daysWithoutDailyClosing: Set<CalendarDate>): List<BreakdownRow> =
    when (selected) {
        BalanceBreakdown.BY_DAY -> summary.byDay.map { day ->
            val payments = payments(day.totals)
            // A day without its closing says so in words, not only with a color.
            val detail = if (day.date in daysWithoutDailyClosing) stringResource(Res.string.balance_day_without_closing_line, payments) else payments
            BreakdownRow(formatCalendarDate(day.date), detail, day.totals.totalReceived)
        }
        BalanceBreakdown.BY_METHOD -> summary.byMethod.map { BreakdownRow(stringResource(it.method.label()), payments(it.totals), it.totals.totalReceived) }
        BalanceBreakdown.BY_EMPLOYEE -> summary.byEmployee.map {
            BreakdownRow(employeeNameOrUnknown(it.employeeName), payments(it.totals), it.totals.totalReceived)
        }
        BalanceBreakdown.BY_PRODUCT -> summary.byProduct.map {
            BreakdownRow(it.productName, stringResource(Res.string.balance_product_line, it.quantity, formatPesos(it.taxAmount)), it.amount)
        }
    }

@Composable
private fun payments(totals: BalanceTotals): String = pluralStringResource(Res.plurals.balance_payments, totals.paymentCount, totals.paymentCount)

private fun BalanceBreakdown.title(): StringResource = when (this) {
    BalanceBreakdown.BY_DAY -> Res.string.balance_by_day
    BalanceBreakdown.BY_METHOD -> Res.string.balance_by_method
    BalanceBreakdown.BY_EMPLOYEE -> Res.string.balance_by_employee
    BalanceBreakdown.BY_PRODUCT -> Res.string.balance_by_product
}
