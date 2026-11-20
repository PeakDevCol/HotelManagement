package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationDraft
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationNumber
import com.peakdevcol.project.hotelmanagement.domain.model.number
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.done
import com.peakdevcol.project.hotelmanagement.resources.destination_number
import com.peakdevcol.project.hotelmanagement.resources.destination_number_optional
import com.peakdevcol.project.hotelmanagement.resources.destination_sheet_title
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.icon
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.stringResource

/** RF-ORD-03: a room or a table with its number, the pool or the bar with an optional one, or "Otro". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DestinationSheet(draft: DestinationDraft, onIntent: (AddOrderIntent) -> Unit) {
    val kind = draft.kind
    TaskSheet(
        title = stringResource(Res.string.destination_sheet_title),
        onDismiss = { onIntent(AddOrderIntent.DismissDestination) },
        asDialog = LocalWideWindow.current,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
        ) {
            DestinationKind.entries.forEach { option ->
                MajesticChip(
                    text = stringResource(option.label()),
                    selected = kind == option,
                    icon = option.icon(),
                    selectedIcon = option.icon(filled = true),
                    onClick = { onIntent(AddOrderIntent.SelectDestinationKind(option)) },
                )
            }
        }
        if (kind != null && kind.number != DestinationNumber.NONE) {
            MajesticTextField(
                value = draft.numberText,
                onValueChange = { onIntent(AddOrderIntent.DestinationNumberChange(it)) },
                label = stringResource(if (kind.number == DestinationNumber.REQUIRED) Res.string.destination_number else Res.string.destination_number_optional),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(AddOrderIntent.ConfirmDestination) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MajesticButton(
            text = stringResource(Res.string.done),
            onClick = { onIntent(AddOrderIntent.ConfirmDestination) },
            size = ButtonSize.Large,
            enabled = draft.destination != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
