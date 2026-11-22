package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.room_inactive
import com.peakdevcol.project.hotelmanagement.resources.room_status_available
import com.peakdevcol.project.hotelmanagement.resources.room_status_cleaning
import com.peakdevcol.project.hotelmanagement.resources.room_status_dirty
import com.peakdevcol.project.hotelmanagement.resources.room_status_occupied
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The narrowest a [RoomTile] gets, so the narrowest phone still shows two rooms side by side. */
val RoomTileMinWidth: Dp = 136.dp

/** The state of a room with a word, an icon and a color, never with color alone (RF-HAB-04). */
@Composable
fun RoomStatusBadge(status: RoomStatus, modifier: Modifier = Modifier) {
    val look = status.look()
    Badge(stringResource(look.label), look.icon, look.container, look.content, modifier)
}

/**
 * A room of the board (RF-HAB-04): its number first and largest, on the color of its state, with
 * the word and the icon of the state. Then its type and, when there is one, a [detail] like the
 * expected check-out. An [inactive] room, which only administrators see, shows that instead of its
 * state. The number shrinks before it is cut.
 */
@Composable
fun RoomTile(
    number: Int,
    typeName: String,
    status: RoomStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    inactive: Boolean = false,
) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val look = if (inactive) inactiveLook() else status.look()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MajesticRadius.large),
        color = look.container,
        contentColor = colors.ink,
        border = BorderStroke(1.dp, colors.line),
    ) {
        Column(
            modifier = Modifier.heightIn(min = ROOM_TILE_MIN_HEIGHT).padding(MajesticSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                AutoSizeText(
                    text = number.toString(),
                    style = typography.headline.copy(fontFeatureSettings = "tnum"),
                    color = colors.ink,
                    minFontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                MajesticIcon(icon = look.icon, contentDescription = null, tint = look.content, size = 20.dp)
            }
            Text(text = stringResource(look.label), style = typography.label, color = look.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = typeName, style = typography.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) {
                Text(text = detail, style = typography.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// Room for the number, the state and the type: rooms without a detail line keep the same height.
private val ROOM_TILE_MIN_HEIGHT = 112.dp

private data class RoomLook(val label: StringResource, val icon: DrawableResource, val container: Color, val content: Color)

@Composable
private fun RoomStatus.look(): RoomLook {
    val colors = MajesticTheme.colors
    return when (this) {
        RoomStatus.AVAILABLE -> RoomLook(Res.string.room_status_available, MajesticIcons.checkCircle, colors.successSoft, colors.success)
        RoomStatus.OCCUPIED -> RoomLook(Res.string.room_status_occupied, MajesticIcons.person, colors.primarySoft, colors.onPrimarySoft)
        RoomStatus.DIRTY -> RoomLook(Res.string.room_status_dirty, MajesticIcons.cleaning, colors.warningSoft, colors.warning)
        RoomStatus.CLEANING -> RoomLook(Res.string.room_status_cleaning, MajesticIcons.timelapse, colors.warningSoft, colors.warning)
    }
}

@Composable
private fun inactiveLook(): RoomLook {
    val colors = MajesticTheme.colors
    return RoomLook(Res.string.room_inactive, MajesticIcons.block, colors.surfaceSunken, colors.inkMuted)
}
