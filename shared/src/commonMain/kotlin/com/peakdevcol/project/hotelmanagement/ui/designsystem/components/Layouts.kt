package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme

/** The narrowest space where a list and its detail fit side by side. */
val ListDetailMinWidth: Dp = MajesticSizes.listPane + MajesticSizes.detailPane

/**
 * A list and the detail of what is chosen in it, side by side, like the orders on desktop. Use it
 * only from [ListDetailMinWidth]; with less room the detail opens as its own screen.
 */
@Composable
fun ListDetailLayout(
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(MajesticSizes.listPane).fillMaxHeight()) { list() }
        VerticalDivider(thickness = 1.dp, color = MajesticTheme.colors.line)
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { detail() }
    }
}

/** A titled group of fields or figures inside a screen, like the start of the operational day. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    help: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MajesticTheme.colors
    val shape = RoundedCornerShape(MajesticRadius.large)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.line, shape)
            .padding(MajesticSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        if (title != null || help != null) {
            Column {
                if (title != null) {
                    Text(text = title, style = MajesticTheme.typography.title, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (help != null) Text(text = help, style = MajesticTheme.typography.body, color = colors.inkMuted)
            }
        }
        content()
    }
}
