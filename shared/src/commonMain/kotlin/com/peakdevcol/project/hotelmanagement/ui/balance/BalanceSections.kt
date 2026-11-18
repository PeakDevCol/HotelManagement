package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceSummary
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceTotals
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.PendingCollection
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSales
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_by_day
import com.peakdevcol.project.hotelmanagement.resources.balance_by_employee
import com.peakdevcol.project.hotelmanagement.resources.balance_by_method
import com.peakdevcol.project.hotelmanagement.resources.balance_by_product
import com.peakdevcol.project.hotelmanagement.resources.balance_day_without_closing
import com.peakdevcol.project.hotelmanagement.resources.balance_empty
import com.peakdevcol.project.hotelmanagement.resources.balance_payment_count
import com.peakdevcol.project.hotelmanagement.resources.balance_pending_detail
import com.peakdevcol.project.hotelmanagement.resources.balance_pending_title
import com.peakdevcol.project.hotelmanagement.resources.balance_product_line
import com.peakdevcol.project.hotelmanagement.resources.balance_product_note
import com.peakdevcol.project.hotelmanagement.resources.balance_product_quantity
import com.peakdevcol.project.hotelmanagement.resources.balance_product_sold
import com.peakdevcol.project.hotelmanagement.resources.balance_sales_without_tax
import com.peakdevcol.project.hotelmanagement.resources.balance_taxes
import com.peakdevcol.project.hotelmanagement.resources.balance_tips
import com.peakdevcol.project.hotelmanagement.resources.balance_total_received
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * RF-BAL-01 and RF-BAL-07. With a product chosen, [productSales] is what that product sold, and the
 * tips are left out because they belong to the whole payment.
 */
@Composable
fun TotalsCard(totals: BalanceTotals, productSales: ProductSales?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (productSales == null) {
                Text(stringResource(Res.string.balance_total_received), style = MaterialTheme.typography.titleSmall)
                Text(formatPesos(totals.totalReceived), style = MaterialTheme.typography.headlineMedium)
            } else {
                Text(stringResource(Res.string.balance_product_sold, productSales.productName), style = MaterialTheme.typography.titleSmall)
                Text(formatPesos(totals.salesAmount), style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                stringResource(Res.string.balance_payment_count, totals.paymentCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            if (productSales != null) AmountRow(stringResource(Res.string.balance_product_quantity), productSales.quantity.toString())
            AmountRow(stringResource(Res.string.balance_sales_without_tax), formatPesos(totals.salesWithoutTax))
            AmountRow(stringResource(Res.string.balance_taxes), formatPesos(totals.taxAmount))
            if (productSales == null) {
                AmountRow(stringResource(Res.string.balance_tips), formatPesos(totals.tipAmount))
            } else {
                Text(
                    stringResource(Res.string.balance_product_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** RF-BAL-04 */
@Composable
fun PendingCard(pending: PendingCollection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AmountRow(stringResource(Res.string.balance_pending_title), formatPesos(pending.totalAmount), emphasized = true)
            Text(
                text = stringResource(
                    Res.string.balance_pending_detail,
                    pending.inProgressCount,
                    formatPesos(pending.inProgressAmount),
                    pending.closedCount,
                    formatPesos(pending.closedAmount),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** RF-BAL-03 */
@Composable
fun Breakdowns(
    summary: BalanceSummary,
    selected: BalanceBreakdown,
    daysWithoutDailyClosing: Set<CalendarDate>,
    onSelect: (BalanceBreakdown) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 0.dp) {
            BalanceBreakdown.entries.forEach { breakdown ->
                Tab(
                    selected = breakdown == selected,
                    onClick = { onSelect(breakdown) },
                    text = { Text(stringResource(breakdown.title())) },
                )
            }
        }
        if (summary.totals.paymentCount == 0) {
            Text(
                stringResource(Res.string.balance_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            return@Column
        }
        when (selected) {
            BalanceBreakdown.BY_DAY -> summary.byDay.forEach { day ->
                TotalsRow(formatCalendarDate(day.date), day.totals, warning = day.date in daysWithoutDailyClosing)
            }
            BalanceBreakdown.BY_METHOD -> summary.byMethod.forEach { method ->
                TotalsRow(stringResource(method.method.label()), method.totals)
            }
            BalanceBreakdown.BY_EMPLOYEE -> summary.byEmployee.forEach { employee ->
                TotalsRow(employeeNameOrUnknown(employee.employeeName), employee.totals)
            }
            BalanceBreakdown.BY_PRODUCT -> summary.byProduct.forEach { product ->
                BreakdownRow(
                    title = product.productName,
                    detail = stringResource(Res.string.balance_product_line, product.quantity, formatPesos(product.taxAmount)),
                    amount = formatPesos(product.amount),
                )
            }
        }
    }
}

@Composable
private fun TotalsRow(title: String, totals: BalanceTotals, warning: Boolean = false) {
    BreakdownRow(
        title = title,
        detail = stringResource(Res.string.balance_payment_count, totals.paymentCount),
        amount = formatPesos(totals.totalReceived),
        warning = if (warning) stringResource(Res.string.balance_day_without_closing) else null,
    )
}

@Composable
private fun BreakdownRow(title: String, detail: String, amount: String, warning: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
        Text(amount, style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider()
}

@Composable
private fun AmountRow(title: String, amount: String, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = style, modifier = Modifier.weight(1f))
        Text(amount, style = style)
    }
}

private fun BalanceBreakdown.title(): StringResource = when (this) {
    BalanceBreakdown.BY_DAY -> Res.string.balance_by_day
    BalanceBreakdown.BY_METHOD -> Res.string.balance_by_method
    BalanceBreakdown.BY_EMPLOYEE -> Res.string.balance_by_employee
    BalanceBreakdown.BY_PRODUCT -> Res.string.balance_by_product
}
