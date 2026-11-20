package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource

enum class BannerTone {
    /** Something waits: orders not sent yet, no connection, days without a cash closing. */
    Warning,

    /** Something could not be loaded or saved. */
    Danger,

    /** A useful explanation that is not a problem. */
    Info,
}

/**
 * A notice about the whole screen, visible until it is solved. It says what is going on and what
 * to do, without apologies.
 */
@Composable
fun Banner(
    tone: BannerTone,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: DrawableResource? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MajesticTheme.colors
    val (container, iconTint, content) = when (tone) {
        BannerTone.Warning -> Triple(colors.warningSoft, colors.warning, colors.ink)
        BannerTone.Danger -> Triple(colors.dangerSoft, colors.danger, colors.ink)
        BannerTone.Info -> Triple(colors.primarySoft, colors.onPrimarySoft, colors.onPrimarySoft)
    }
    val shownIcon = icon ?: when (tone) {
        BannerTone.Warning -> MajesticIcons.warning
        BannerTone.Danger -> MajesticIcons.error
        BannerTone.Info -> MajesticIcons.info
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(MajesticRadius.medium))
            .padding(start = MajesticSpacing.space4, top = MajesticSpacing.space3, end = if (actionText != null) MajesticSpacing.space1 else MajesticSpacing.space4, bottom = MajesticSpacing.space3),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MajesticIcon(icon = shownIcon, contentDescription = null, tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) Text(text = title, style = MajesticTheme.typography.label, color = content)
            Text(text = message, style = MajesticTheme.typography.body, color = content)
        }
        if (actionText != null && onAction != null) {
            MajesticButton(text = actionText, onClick = onAction, style = ButtonStyle.Text)
        }
    }
}

/** Something is being read. It takes the space it is given and centers itself in it. */
@Composable
fun MajesticLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MajesticTheme.colors.primary, trackColor = MajesticTheme.colors.surfaceSunken)
    }
}

/** Why there is nothing here, and the next step. */
@Composable
fun EmptyState(
    icon: DrawableResource,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionIcon: DrawableResource? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MajesticTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MajesticSpacing.space6, vertical = MajesticSpacing.space10),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(colors.primarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MajesticIcon(icon = icon, contentDescription = null, tint = colors.onPrimarySoft, size = 32.dp)
        }
        Text(text = title, style = MajesticTheme.typography.title, color = colors.ink, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MajesticTheme.typography.body,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionText != null && onAction != null) {
            MajesticButton(text = actionText, onClick = onAction, icon = actionIcon)
        }
    }
}

/** Short confirmations ("Pago registrado"), with an optional action. Never for errors that need a fix. */
@Composable
fun MajesticSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = MajesticTheme.colors
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(MajesticRadius.medium),
            containerColor = colors.inverseSurface,
            contentColor = colors.onInverseSurface,
            actionColor = colors.inversePrimary,
            dismissActionContentColor = colors.onInverseSurface,
        )
    }
}
