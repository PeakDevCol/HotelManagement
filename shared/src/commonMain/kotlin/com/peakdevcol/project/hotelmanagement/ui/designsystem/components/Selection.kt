package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.ds_count_decrease
import com.peakdevcol.project.hotelmanagement.resources.ds_count_increase
import com.peakdevcol.project.hotelmanagement.resources.ds_quantity_decrease
import com.peakdevcol.project.hotelmanagement.resources.ds_quantity_increase
import com.peakdevcol.project.hotelmanagement.resources.ds_quantity_remove
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/**
 * Adds or takes away units without opening the keyboard (RF-ORD-01: 1 to 99). At 1, the minus
 * becomes "Quitar", which takes the product out of the order. It is compact unless it is given a
 * width; then the buttons go to the ends and the number stays in the middle.
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    maximum: Int = 99,
) {
    val colors = MajesticTheme.colors
    Row(
        modifier = modifier
            .background(colors.surfaceRaised, RoundedCornerShape(MajesticRadius.medium))
            .border(1.dp, colors.lineStrong, RoundedCornerShape(MajesticRadius.medium)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (quantity <= 1) {
            MajesticIconButton(icon = MajesticIcons.delete, contentDescription = stringResource(Res.string.ds_quantity_remove), onClick = onRemove, tint = colors.danger)
        } else {
            MajesticIconButton(icon = MajesticIcons.remove, contentDescription = stringResource(Res.string.ds_quantity_decrease), onClick = onDecrease, tint = colors.primary)
        }
        Text(
            text = quantity.toString(),
            style = MajesticTheme.typography.amount.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize),
            color = colors.ink,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp),
        )
        MajesticIconButton(
            icon = MajesticIcons.add,
            contentDescription = stringResource(Res.string.ds_quantity_increase),
            onClick = onIncrease,
            tint = colors.primary,
            enabled = quantity < maximum,
        )
    }
}

/**
 * How many of something there are, from [minimum] to [maximum], with its name beside it, like the
 * adults and the children of a reservation (RF-RES-02). Unlike [QuantityStepper], the minus never
 * takes anything out: it stops at [minimum].
 */
@Composable
fun CountStepper(
    label: String,
    count: Int,
    onCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimum: Int = 0,
    maximum: Int = 99,
) {
    val colors = MajesticTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = MajesticSizes.control),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MajesticTheme.typography.bodyLarge,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .background(colors.surfaceRaised, RoundedCornerShape(MajesticRadius.medium))
                .border(1.dp, colors.lineStrong, RoundedCornerShape(MajesticRadius.medium)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MajesticIconButton(
                icon = MajesticIcons.remove,
                contentDescription = stringResource(Res.string.ds_count_decrease, label),
                onClick = { onCountChange(count - 1) },
                tint = colors.primary,
                enabled = count > minimum,
            )
            Text(
                text = count.toString(),
                style = MajesticTheme.typography.amount.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize),
                color = colors.ink,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 32.dp),
            )
            MajesticIconButton(
                icon = MajesticIcons.add,
                contentDescription = stringResource(Res.string.ds_count_increase, label),
                onClick = { onCountChange(count + 1) },
                tint = colors.primary,
                enabled = count < maximum,
            )
        }
    }
}

/** Turns something on or off, like an active product. The whole row responds to the touch. */
@Composable
fun MajesticSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    help: String? = null,
    enabled: Boolean = true,
) {
    val colors = MajesticTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MajesticSizes.control)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = MajesticSpacing.space2),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MajesticTheme.typography.bodyLarge, color = if (enabled) colors.ink else colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (help != null) Text(text = help, style = MajesticTheme.typography.caption, color = colors.inkMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = colors.lineStrong,
                uncheckedTrackColor = colors.surfaceSunken,
                uncheckedBorderColor = colors.lineStrong,
                disabledCheckedThumbColor = colors.surfaceRaised,
                disabledCheckedTrackColor = colors.lineStrong,
                disabledUncheckedThumbColor = colors.line,
                disabledUncheckedTrackColor = colors.surfaceSunken,
                disabledUncheckedBorderColor = colors.line,
            ),
        )
    }
}

/**
 * One choice among two to four that exclude each other, like the payment method. The chosen
 * segment is raised and carries a check, so it does not depend on color alone.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = MajesticTheme.colors
    Row(
        modifier = modifier
            .background(colors.surfaceSunken, RoundedCornerShape(MajesticRadius.medium))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MajesticSizes.control)
                    .then(if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(MajesticRadius.small)) else Modifier)
                    .background(if (isSelected) colors.surfaceRaised else Color.Transparent, RoundedCornerShape(MajesticRadius.small))
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = MajesticSpacing.space3),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) MajesticIcon(icon = MajesticIcons.check, contentDescription = null, tint = colors.ink, size = 18.dp)
                // Two lines before an ellipsis: "Cierre del día" and "Cierre de turno" must not
                // both become "Cierre d…" with large text.
                Text(
                    text = label(option),
                    style = MajesticTheme.typography.label,
                    color = if (isSelected) colors.ink else colors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false).padding(vertical = MajesticSpacing.space1),
                )
            }
        }
    }
}

/**
 * Filters a list or picks a quick value, like a destination or a suggested tip. The chosen chip
 * uses `primary-soft` and shows a check, or its own icon filled.
 */
@Composable
fun MajesticChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    selectedIcon: DrawableResource? = icon,
    count: Int? = null,
) {
    val colors = MajesticTheme.colors
    Surface(
        selected = selected,
        onClick = onClick,
        // As wide as its whole label when there is room, also in a row that scrolls sideways; on a
        // line too narrow for it, the label is the part that gets shortened.
        modifier = modifier.minimumInteractiveComponentSize().heightIn(min = 40.dp).width(IntrinsicSize.Max),
        shape = RoundedCornerShape(MajesticRadius.small),
        color = if (selected) colors.primarySoft else colors.surfaceRaised,
        contentColor = if (selected) colors.onPrimarySoft else colors.ink,
        border = if (selected) null else BorderStroke(1.dp, colors.lineStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MajesticSpacing.space4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shownIcon = if (selected) selectedIcon ?: MajesticIcons.check else icon
            if (shownIcon != null) MajesticIcon(icon = shownIcon, contentDescription = null, size = 18.dp)
            // A label longer than the line is shortened, and the count after it stays in sight.
            Text(
                text = text,
                style = MajesticTheme.typography.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MajesticTheme.typography.label.copy(fontFeatureSettings = "tnum"),
                    color = if (selected) colors.onPrimarySoft else colors.inkMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The filters of a list on a phone: one row that scrolls sideways and fades at the right edge, so
 * it is clear there is more. With a pointer, use a wrapping group instead.
 */
@Composable
fun ScrollingChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeWidth = FADE_WIDTH.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = size.width - fadeWidth,
                        endX = size.width,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            .horizontalScroll(rememberScrollState())
            .padding(end = MajesticSpacing.space6),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private val FADE_WIDTH = 32.dp
