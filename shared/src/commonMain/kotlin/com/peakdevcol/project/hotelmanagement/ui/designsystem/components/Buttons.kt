package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource

enum class ButtonStyle {
    /** The one main action of a screen. */
    Primary,

    /** A frequent supporting action, like adding products. */
    Tonal,

    /** The alternative to the main action, like reopening. */
    Outline,

    /** A minor action, and the way out of a dialog. */
    Text,

    /** What cannot be undone, always after a confirmation. */
    Danger,
    DangerOutline,
}

enum class ButtonSize {
    Regular,

    /** The main action of an operational screen, full width at the bottom on a phone. */
    Large,
}

private data class ButtonColors(val container: Color, val content: Color, val border: Color?)

@Composable
private fun ButtonStyle.colors(): ButtonColors {
    val colors = MajesticTheme.colors
    return when (this) {
        ButtonStyle.Primary -> ButtonColors(colors.primary, colors.onPrimary, null)
        ButtonStyle.Tonal -> ButtonColors(colors.primarySoft, colors.onPrimarySoft, null)
        ButtonStyle.Outline -> ButtonColors(Color.Transparent, colors.primary, colors.lineStrong)
        ButtonStyle.Text -> ButtonColors(Color.Transparent, colors.primary, null)
        ButtonStyle.Danger -> ButtonColors(colors.danger, colors.onDanger, null)
        ButtonStyle.DangerOutline -> ButtonColors(Color.Transparent, colors.danger, colors.lineStrong)
    }
}

/**
 * A button of the system. The text is a verb that says what happens, and it is shortened with an
 * ellipsis before it pushes anything out of place. While [loading], it shows a progress indicator
 * and takes no more touches.
 */
@Composable
fun MajesticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    size: ButtonSize = ButtonSize.Regular,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = style.colors()
    val large = size == ButtonSize.Large
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .defaultMinSize(minHeight = if (large) MajesticSizes.controlLarge else MajesticSizes.control)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        shape = RoundedCornerShape(MajesticRadius.medium),
        color = colors.container,
        contentColor = colors.content,
        border = colors.border?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = when {
                    style == ButtonStyle.Text -> MajesticSpacing.space3
                    large -> MajesticSpacing.space6
                    else -> MajesticSpacing.space5
                },
            ),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.content,
                    strokeWidth = 2.dp,
                )
                icon != null -> MajesticIcon(icon = icon, contentDescription = null, size = 20.dp)
            }
            val label = MajesticTheme.typography.label
            Text(
                text = text,
                style = if (large) label.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize, lineHeight = MajesticTheme.typography.bodyLarge.lineHeight) else label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/** A button with only an icon. It always has a description for screen readers. */
@Composable
fun MajesticIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier.size(MajesticSizes.control), enabled = enabled) {
        MajesticIcon(icon = icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * Opens the most frequent task of a section, one per screen. The list under it ends with
 * [MajesticSizes.floatingActionButtonClearance] of room, so it never covers the last item.
 */
@Composable
fun MajesticFloatingActionButton(
    text: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val colors = MajesticTheme.colors
    ExtendedFloatingActionButton(
        text = {
            Text(text = text, style = MajesticTheme.typography.label.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        icon = { MajesticIcon(icon = icon, contentDescription = if (expanded) null else text) },
        onClick = onClick,
        expanded = expanded,
        modifier = modifier,
        shape = RoundedCornerShape(MajesticRadius.large),
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(),
    )
}

private const val DISABLED_ALPHA = 0.38f
