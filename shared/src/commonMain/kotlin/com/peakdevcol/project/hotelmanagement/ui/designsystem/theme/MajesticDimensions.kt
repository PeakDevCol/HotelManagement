package com.peakdevcol.project.hotelmanagement.ui.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing on a 4 dp base. Groups are separated with space, not with lines. */
object MajesticSpacing {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp
    val space8 = 32.dp
    val space10 = 40.dp
    val space12 = 48.dp
}

/** Quiet curves: 12 on controls, 16 on cards. Pills only for status and the active navigation. */
object MajesticRadius {
    val small = 10.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
}

/** Fixed measures. Everything that is touched is at least [control] tall (RNF-08). */
object MajesticSizes {
    val control = 48.dp
    val controlLarge = 56.dp
    val icon = 24.dp
    val avatar = 40.dp
    val topBar = 64.dp
    val navigationBar = 80.dp
    val sideNavigation = 256.dp
    val sideNavigationCompact = 88.dp
    val content = 720.dp

    /** From this width on, side navigation instead of the bottom bar. */
    val mediumWidth = 600.dp

    /** From this width on, full side navigation and list and detail side by side. */
    val expandedWidth = 840.dp

    /** Room left at the end of a list under a floating action button, so it never covers the last item. */
    val floatingActionButtonClearance = controlLarge + MajesticSpacing.space8
}

internal val MajesticShapes = Shapes(
    extraSmall = RoundedCornerShape(MajesticRadius.small),
    small = RoundedCornerShape(MajesticRadius.small),
    medium = RoundedCornerShape(MajesticRadius.medium),
    large = RoundedCornerShape(MajesticRadius.large),
    extraLarge = RoundedCornerShape(MajesticRadius.extraLarge),
)
