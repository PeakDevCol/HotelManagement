package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource

/** Rows grouped in one card, separated by hairlines. Put a [ListDivider] between rows. */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MajesticTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MajesticRadius.large))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.line, RoundedCornerShape(MajesticRadius.large)),
        content = content,
    )
}

/**
 * One item with its main data at the end, like an employee or a product of the inventory. Title
 * and subtitle take one line each and are cut with an ellipsis; the end data keeps its size.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MajesticTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = MajesticSpacing.space4, vertical = MajesticSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MajesticTheme.typography.bodyLarge.copy(fontWeight = MajesticTheme.typography.caption.fontWeight), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(text = subtitle, style = MajesticTheme.typography.body, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

/** A circle with the initials of a person, or an icon on the neutral ground. */
@Composable
fun Avatar(modifier: Modifier = Modifier, initials: String? = null, icon: DrawableResource? = null) {
    val colors = MajesticTheme.colors
    val neutral = initials == null
    Box(
        modifier = modifier
            .size(MajesticSizes.avatar)
            .background(if (neutral) colors.surfaceSunken else colors.primarySoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            initials != null -> Text(text = initials, style = MajesticTheme.typography.label.copy(fontWeight = MajesticTheme.typography.amount.fontWeight), color = colors.onPrimarySoft, maxLines = 1)
            icon != null -> MajesticIcon(icon = icon, contentDescription = null, tint = colors.inkMuted)
        }
    }
}

/** The first letter of the first two words of a name: "Ana María Rojas" is "AM". */
fun initialsOf(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).filter(String::isNotEmpty).take(2)
        .joinToString("") { it.first().uppercase() }

enum class StatEmphasis {
    /** The main figure of the screen, on the brand color. Once per screen. */
    Brand,
    Normal,
}

/** A figure with its label and a detail that gives it context. */
data class StatTileData(
    val label: String,
    val value: String,
    val detail: String? = null,
    val emphasis: StatEmphasis = StatEmphasis.Normal,
)

/**
 * A row of figure tiles. Each tile is at least 200 dp wide (320 dp the brand one) and the tiles
 * move to the next line before getting narrower; the figure shrinks with its tile, never cut.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatTileRow(tiles: List<StatTileData>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        tiles.forEach { tile ->
            val brand = tile.emphasis == StatEmphasis.Brand
            StatTile(
                tile = tile,
                modifier = Modifier
                    .widthIn(min = if (brand) STAT_TILE_BRAND_MIN_WIDTH else STAT_TILE_MIN_WIDTH)
                    .weight(if (brand) 2f else 1f),
            )
        }
    }
}

@Composable
fun StatTile(tile: StatTileData, modifier: Modifier = Modifier) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val brand = tile.emphasis == StatEmphasis.Brand
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MajesticRadius.large))
            .background(if (brand) colors.brand else colors.surfaceRaised)
            .then(if (brand) Modifier else Modifier.border(1.dp, colors.line, RoundedCornerShape(MajesticRadius.large)))
            .padding(MajesticSpacing.space5),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1),
    ) {
        SectionLabel(text = tile.label, color = if (brand) colors.onBrandMuted else colors.inkMuted)
        AutoSizeText(
            text = tile.value,
            style = if (brand) typography.display else typography.headline.copy(fontWeight = typography.amount.fontWeight, fontFeatureSettings = "tnum"),
            color = if (brand) colors.onBrand else colors.ink,
            minFontSize = if (brand) 28.sp else 20.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        if (tile.detail != null) {
            Text(text = tile.detail, style = typography.caption, color = if (brand) colors.onBrandMuted else colors.inkMuted)
        }
    }
}

private val STAT_TILE_MIN_WIDTH = 200.dp
private val STAT_TILE_BRAND_MIN_WIDTH = 320.dp
