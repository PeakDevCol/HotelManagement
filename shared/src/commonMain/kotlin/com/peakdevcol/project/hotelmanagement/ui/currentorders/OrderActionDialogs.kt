package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.order_cancel_action
import com.peakdevcol.project.hotelmanagement.resources.order_cancel_keep
import com.peakdevcol.project.hotelmanagement.resources.order_cancel_message
import com.peakdevcol.project.hotelmanagement.resources.order_cancel_reason
import com.peakdevcol.project.hotelmanagement.resources.order_payment_method_cash
import com.peakdevcol.project.hotelmanagement.resources.order_payment_method_transfer
import com.peakdevcol.project.hotelmanagement.resources.order_payment_tip
import com.peakdevcol.project.hotelmanagement.resources.order_payment_title
import com.peakdevcol.project.hotelmanagement.resources.order_payment_total
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun PaymentMethod.label(): StringResource = when (this) {
    PaymentMethod.CASH -> Res.string.order_payment_method_cash
    PaymentMethod.TRANSFER -> Res.string.order_payment_method_transfer
}

@Composable
fun ConfirmActionDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.dialog_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** RF-CUR-05: the payment is for the whole order; the method is required and the tip is optional. */
@Composable
fun PaymentDialog(
    orderAmount: Long,
    onSubmit: (method: PaymentMethod, tipAmount: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var method by rememberSaveable { mutableStateOf<PaymentMethod?>(null) }
    var tipText by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.order_payment_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.order_payment_total, formatPesos(orderAmount)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(modifier = Modifier.selectableGroup()) {
                    PaymentMethod.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = method == option, onClick = { method = option }, role = Role.RadioButton)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = method == option, onClick = null)
                            Text(stringResource(option.label()), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = tipText,
                    onValueChange = { text -> tipText = text.filter(Char::isDigit).take(MAX_TIP_DIGITS) },
                    label = { Text(stringResource(Res.string.order_payment_tip)) },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { method?.let { onSubmit(it, tipText.toLongOrNull() ?: 0) } },
                enabled = method != null,
            ) { Text(stringResource(Res.string.dialog_accept)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** RF-CUR-07: an order is cancelled only with a reason, which stays in its history. */
@Composable
fun CancelOrderDialog(onSubmit: (reason: String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.order_cancel_action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.order_cancel_message))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(MAX_REASON_LENGTH) },
                    label = { Text(stringResource(Res.string.order_cancel_reason)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason) }, enabled = reason.isNotBlank()) {
                Text(stringResource(Res.string.order_cancel_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.order_cancel_keep)) } },
    )
}

private const val MAX_TIP_DIGITS = 7
private const val MAX_REASON_LENGTH = 200
