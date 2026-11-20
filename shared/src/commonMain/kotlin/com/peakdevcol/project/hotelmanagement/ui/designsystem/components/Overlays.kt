package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme

/**
 * A short task without leaving the screen, like choosing the destination or charging an order.
 * The main action goes last, large. On desktop the same task is shown as a dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MajesticBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MajesticTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = MajesticRadius.extraLarge, topEnd = MajesticRadius.extraLarge),
        containerColor = colors.surfaceRaised,
        contentColor = colors.ink,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(Modifier.padding(top = MajesticSpacing.space3, bottom = MajesticSpacing.space2).size(width = 32.dp, height = 4.dp).background(colors.lineStrong, RoundedCornerShape(percent = 50)))
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = MajesticSpacing.space6, end = MajesticSpacing.space6, bottom = MajesticSpacing.space6),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
        ) {
            Column {
                Text(text = title, style = MajesticTheme.typography.titleLarge, color = colors.ink)
                if (subtitle != null) Text(text = subtitle, style = MajesticTheme.typography.body, color = colors.inkMuted)
            }
            content()
        }
    }
}

/**
 * Asks to confirm a decision that cannot be undone. The title is a question, the message says the
 * consequence with the figures that matter.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = MajesticTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(MajesticRadius.extraLarge),
        containerColor = colors.surfaceRaised,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        title = { Text(text = title, style = MajesticTheme.typography.titleLarge) },
        text = { Text(text = message, style = MajesticTheme.typography.body) },
        confirmButton = {
            MajesticButton(text = confirmText, onClick = onConfirm, style = if (destructive) ButtonStyle.Danger else ButtonStyle.Primary)
        },
        dismissButton = {
            MajesticButton(text = dismissText, onClick = onDismiss, style = ButtonStyle.Text)
        },
    )
}

/** A dialog version of a short task for wide windows, with the same title and content as the sheet. */
@Composable
fun TaskDialog(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MajesticTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(MajesticRadius.extraLarge),
        containerColor = colors.surfaceRaised,
        title = {
            Column {
                Text(text = title, style = MajesticTheme.typography.titleLarge, color = colors.ink)
                if (subtitle != null) Text(text = subtitle, style = MajesticTheme.typography.body, color = colors.inkMuted)
            }
        },
        text = { Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4), horizontalAlignment = Alignment.Start, content = content) },
        confirmButton = {},
    )
}
