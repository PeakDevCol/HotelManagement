package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.order_employee_unknown
import com.peakdevcol.project.hotelmanagement.resources.order_pending_to_send
import com.peakdevcol.project.hotelmanagement.resources.order_state_cancelled
import com.peakdevcol.project.hotelmanagement.resources.order_state_closed
import com.peakdevcol.project.hotelmanagement.resources.order_state_in_progress
import com.peakdevcol.project.hotelmanagement.resources.order_state_paid
import com.peakdevcol.project.hotelmanagement.resources.retry
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun OrderState.label(): StringResource = when (this) {
    OrderState.IN_PROGRESS -> Res.string.order_state_in_progress
    OrderState.CLOSED -> Res.string.order_state_closed
    OrderState.PAID -> Res.string.order_state_paid
    OrderState.CANCELLED -> Res.string.order_state_cancelled
}

@Composable
fun employeeNameOrUnknown(name: String?): String = name ?: stringResource(Res.string.order_employee_unknown)

@Composable
fun OrderStateBadge(state: OrderState) {
    val open = state == OrderState.IN_PROGRESS
    Badge(
        text = stringResource(state.label()),
        container = if (open) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        content = if (open) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** RF-ORD-09: the order is saved on this device and the server does not have it yet. */
@Composable
fun PendingToSendBadge() {
    Badge(
        text = stringResource(Res.string.order_pending_to_send),
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun CenteredMessage(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
        }
    }
}
