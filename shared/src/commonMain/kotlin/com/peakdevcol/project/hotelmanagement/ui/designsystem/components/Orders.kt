package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.order_pending_to_send
import com.peakdevcol.project.hotelmanagement.resources.order_state_cancelled
import com.peakdevcol.project.hotelmanagement.resources.order_state_closed
import com.peakdevcol.project.hotelmanagement.resources.order_state_in_progress
import com.peakdevcol.project.hotelmanagement.resources.order_state_paid
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/** Where an order is, with a word, an icon and a color: never with color alone. */
@Composable
fun StatusBadge(state: OrderState, modifier: Modifier = Modifier) {
    val colors = MajesticTheme.colors
    when (state) {
        OrderState.IN_PROGRESS -> Badge(stringResource(Res.string.order_state_in_progress), MajesticIcons.timelapse, colors.primarySoft, colors.onPrimarySoft, modifier)
        OrderState.CLOSED -> Badge(stringResource(Res.string.order_state_closed), MajesticIcons.receipt, colors.warningSoft, colors.warning, modifier)
        OrderState.PAID -> Badge(stringResource(Res.string.order_state_paid), MajesticIcons.checkCircle, colors.successSoft, colors.success, modifier)
        OrderState.CANCELLED -> Badge(stringResource(Res.string.order_state_cancelled), MajesticIcons.block, colors.surfaceSunken, colors.inkMuted, modifier)
    }
}

/** RF-ORD-09: the order is only on this device. It goes next to the state, not instead of it. */
@Composable
fun PendingToSendBadge(modifier: Modifier = Modifier) {
    val colors = MajesticTheme.colors
    Badge(stringResource(Res.string.order_pending_to_send), MajesticIcons.cloudUpload, Color.Transparent, colors.warning, modifier, outlined = true)
}

@Composable
private fun Badge(
    text: String,
    icon: DrawableResource,
    container: Color,
    content: Color,
    modifier: Modifier,
    outlined: Boolean = false,
) {
    Row(
        modifier = modifier
            .heightIn(min = 24.dp)
            .background(container, RoundedCornerShape(percent = 50))
            .then(if (outlined) Modifier.border(1.dp, content, RoundedCornerShape(percent = 50)) else Modifier)
            .padding(start = MajesticSpacing.space2, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MajesticIcon(icon = icon, contentDescription = null, tint = content, size = 16.dp)
        Text(text = text, style = MajesticTheme.typography.caption.copy(fontWeight = MajesticTheme.typography.label.fontWeight), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * An open order, to find it and open it with one touch: the destination first and largest, its
 * state, who took it, how long ago and the total. [selected] marks the one open beside the list.
 */
@Composable
fun OrderCard(
    destination: String,
    destinationIcon: DrawableResource,
    state: OrderState,
    meta: String,
    age: String,
    totalAmount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPendingToSend: Boolean = false,
    selected: Boolean = false,
) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MajesticRadius.large),
        color = colors.surfaceRaised,
        contentColor = colors.ink,
        border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.line),
    ) {
        Column(modifier = Modifier.padding(MajesticSpacing.space4), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
                ) {
                    MajesticIcon(icon = destinationIcon, contentDescription = null, tint = colors.inkMuted, size = 22.dp)
                    Text(text = destination, style = typography.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                StatusBadge(state)
            }
            // On its own line, so on a narrow phone with large text it never squeezes the destination.
            if (isPendingToSend) PendingToSendBadge()
            Text(text = meta, style = typography.body, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MajesticSpacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
            ) {
                Text(text = age, style = typography.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                AmountText(amount = totalAmount)
            }
        }
    }
}

/** The narrowest a [ProductTile] gets. Product grids are built with columns of at least this width. */
val ProductTileMinWidth: Dp = 148.dp

// Room for two lines of name, the price and the quantity stepper: every tile keeps it, so adding
// a product never changes the height of its tile or moves the grid.
private val PRODUCT_TILE_MIN_HEIGHT = 148.dp
private val STEPPER_ROOM = MajesticSizes.control + MajesticSpacing.space2

/**
 * Adds a product to the order with one touch. Once in the order, the quantity stepper takes its
 * own row across the bottom of the tile, so nothing ever sits beside it. The name takes up to two
 * lines.
 */
@Composable
fun ProductTile(
    name: String,
    price: Long,
    quantity: Int,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val inOrder = quantity > 0
    Surface(
        onClick = onAdd,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MajesticRadius.large),
        color = colors.surfaceRaised,
        contentColor = colors.ink,
        border = if (inOrder) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.line),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PRODUCT_TILE_MIN_HEIGHT)
                .padding(start = MajesticSpacing.space4, top = MajesticSpacing.space3, end = MajesticSpacing.space3, bottom = MajesticSpacing.space3),
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(bottom = if (inOrder) STEPPER_ROOM else 0.dp),
                verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1),
            ) {
                Text(text = name, style = typography.bodyLarge.copy(fontWeight = typography.title.fontWeight), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = formatPesos(price), style = typography.label.copy(fontFeatureSettings = "tnum"), color = colors.inkMuted, maxLines = 1)
            }
            if (inOrder) {
                QuantityStepper(
                    quantity = quantity,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                    onRemove = onRemove,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The total of an order and the action to confirm it, fixed at the bottom. The [detail] is cut
 * with an ellipsis before it pushes the button; the total shrinks but is never cut.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderSummaryBar(
    detail: String,
    totalAmount: Long,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    actionLoading: Boolean = false,
) {
    val colors = MajesticTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
        ListDivider()
        // With little width and large text, the button moves under the total instead of squeezing it.
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MajesticSpacing.space4, vertical = MajesticSpacing.space3),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space4, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.widthIn(min = SUMMARY_TOTAL_MIN_WIDTH).weight(1f)) {
                Text(text = detail, style = MajesticTheme.typography.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                AmountText(amount = totalAmount, color = colors.ink, textAlign = TextAlign.Start)
            }
            MajesticButton(text = actionText, onClick = onAction, size = ButtonSize.Large, enabled = actionEnabled, loading = actionLoading)
        }
    }
}

private val SUMMARY_TOTAL_MIN_WIDTH = 140.dp
