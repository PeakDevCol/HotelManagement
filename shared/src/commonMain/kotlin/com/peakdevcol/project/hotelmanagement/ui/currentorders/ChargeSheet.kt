package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.order_charge_closes
import com.peakdevcol.project.hotelmanagement.resources.order_charge_method
import com.peakdevcol.project.hotelmanagement.resources.order_charge_submit
import com.peakdevcol.project.hotelmanagement.resources.order_charge_subtitle
import com.peakdevcol.project.hotelmanagement.resources.order_charge_title
import com.peakdevcol.project.hotelmanagement.resources.order_charge_total
import com.peakdevcol.project.hotelmanagement.resources.order_payment_tip
import com.peakdevcol.project.hotelmanagement.resources.order_tip_amount
import com.peakdevcol.project.hotelmanagement.resources.order_tip_none
import com.peakdevcol.project.hotelmanagement.resources.order_tip_other
import com.peakdevcol.project.hotelmanagement.resources.order_tip_suggested
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MoneyField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SegmentedControl
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.stringResource

/**
 * RF-CUR-05: charging an order without leaving it. An order in progress is closed when the payment
 * is recorded (PRD, section 13, 2026-11-19).
 */
@Composable
fun ChargeSheet(state: OrderDetailState.Loaded, form: ChargeForm, onIntent: (OrderDetailIntent) -> Unit) {
    val order = state.detail.order
    TaskSheet(
        title = stringResource(Res.string.order_charge_title, order.destination.label()),
        subtitle = stringResource(
            Res.string.order_charge_subtitle,
            productCountLabel(order.products.sumOf { it.quantity }),
            formatPesos(order.totalAmount),
        ),
        onDismiss = { onIntent(OrderDetailIntent.DismissDialog) },
        asDialog = LocalWideWindow.current,
    ) {
        ChargeFields(state = state, form = form, onIntent = onIntent)
        if (order.state == OrderState.IN_PROGRESS) {
            Text(text = stringResource(Res.string.order_charge_closes), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
        }
        MajesticButton(
            text = stringResource(Res.string.order_charge_submit),
            onClick = { onIntent(OrderDetailIntent.SubmitCharge) },
            size = ButtonSize.Large,
            enabled = state.canSubmitCharge,
            loading = state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChargeFields(state: OrderDetailState.Loaded, form: ChargeForm, onIntent: (OrderDetailIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = stringResource(Res.string.order_charge_method))
        SegmentedControl(
            options = PaymentMethod.entries,
            selected = form.method,
            onSelect = { onIntent(OrderDetailIntent.SelectPaymentMethod(it)) },
            label = { stringResource(it.label()) },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = stringResource(Res.string.order_payment_tip))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
        ) {
            TipChip(text = stringResource(Res.string.order_tip_none), tip = TipChoice.NONE, form = form, onIntent = onIntent)
            // A suggestion of $0 says nothing: small orders offer only the other two.
            if (state.suggestedTip > 0) {
                TipChip(text = stringResource(Res.string.order_tip_suggested, formatPesos(state.suggestedTip)), tip = TipChoice.SUGGESTED, form = form, onIntent = onIntent)
            }
            TipChip(text = stringResource(Res.string.order_tip_other), tip = TipChoice.OTHER, form = form, onIntent = onIntent)
        }
        if (form.tip == TipChoice.OTHER) {
            MoneyField(
                digits = form.otherTipDigits,
                onDigitsChange = { onIntent(OrderDetailIntent.OtherTipChange(it)) },
                label = stringResource(Res.string.order_tip_amount),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    ListDivider()
    AmountRow(label = stringResource(Res.string.order_charge_total), amount = state.amountToReceive, emphasized = true)
}

@Composable
private fun TipChip(text: String, tip: TipChoice, form: ChargeForm, onIntent: (OrderDetailIntent) -> Unit) {
    MajesticChip(text = text, selected = form.tip == tip, onClick = { onIntent(OrderDetailIntent.SelectTip(tip)) })
}
