package com.peakdevcol.project.hotelmanagement.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.markableAs
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.destination_room_number
import com.peakdevcol.project.hotelmanagement.resources.room_error_missing_type
import com.peakdevcol.project.hotelmanagement.resources.rooms_add
import com.peakdevcol.project.hotelmanagement.resources.rooms_edit
import com.peakdevcol.project.hotelmanagement.resources.rooms_expected_check_out
import com.peakdevcol.project.hotelmanagement.resources.rooms_floor
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_active
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_active_hint
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_edit_title
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_floor
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_new_hint
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_number
import com.peakdevcol.project.hotelmanagement.resources.rooms_form_type
import com.peakdevcol.project.hotelmanagement.resources.rooms_inactive_hint
import com.peakdevcol.project.hotelmanagement.resources.rooms_last_change
import com.peakdevcol.project.hotelmanagement.resources.rooms_last_change_without_name
import com.peakdevcol.project.hotelmanagement.resources.rooms_mark_clean
import com.peakdevcol.project.hotelmanagement.resources.rooms_mark_clean_hint
import com.peakdevcol.project.hotelmanagement.resources.rooms_mark_dirty
import com.peakdevcol.project.hotelmanagement.resources.rooms_mark_dirty_hint
import com.peakdevcol.project.hotelmanagement.resources.rooms_occupied_hint
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSwitchRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomStatusBadge
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RoomDetailSheet(room: BoardRoom, state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    TaskSheet(
        title = stringResource(Res.string.destination_room_number, room.room.number),
        subtitle = room.summary(),
        onDismiss = { onIntent(RoomsIntent.CloseRoom) },
        asDialog = LocalWideWindow.current,
    ) {
        RoomDetailFields(room = room, state = state, onIntent = onIntent)
    }
}

/** RF-HAB-03 and RF-HAB-04: how a room is, who changed its cleaning last and what can be done with it. */
@Composable
fun RoomDetailFields(room: BoardRoom, state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val data = room.room
    if (data.active) {
        RoomStatusBadge(status = room.status)
    } else {
        Text(text = stringResource(Res.string.rooms_inactive_hint), style = typography.body, color = colors.inkMuted)
    }
    data.expectedCheckOutDate?.takeIf { data.occupancy == Occupancy.OCCUPIED }?.let { date ->
        Text(text = stringResource(Res.string.rooms_expected_check_out, formatWeekday(date)), style = typography.body, color = colors.ink)
    }
    room.lastChangeLabel()?.let { Text(text = it, style = typography.caption, color = colors.inkMuted) }
    HousekeepingAction(room = room, marking = state.markingRoomId == data.id, onIntent = onIntent)
    if (state.board.canManage) {
        MajesticButton(
            text = stringResource(Res.string.rooms_edit),
            onClick = { onIntent(RoomsIntent.EditRoom(data.id)) },
            style = ButtonStyle.Text,
            icon = MajesticIcons.edit,
        )
    }
}

/** Decision 5A: marking clean is the main action of a room to be cleaned; marking it back undoes a mistake. */
@Composable
private fun HousekeepingAction(room: BoardRoom, marking: Boolean, onIntent: (RoomsIntent) -> Unit) {
    val data = room.room
    when (data.markableAs) {
        Housekeeping.CLEAN -> MarkButton(
            text = stringResource(Res.string.rooms_mark_clean),
            hint = stringResource(Res.string.rooms_mark_clean_hint),
            style = ButtonStyle.Primary,
            marking = marking,
            onClick = { onIntent(RoomsIntent.MarkHousekeeping(data.id, Housekeeping.CLEAN)) },
        )
        Housekeeping.DIRTY -> MarkButton(
            text = stringResource(Res.string.rooms_mark_dirty),
            hint = stringResource(Res.string.rooms_mark_dirty_hint),
            style = ButtonStyle.Outline,
            marking = marking,
            onClick = { onIntent(RoomsIntent.MarkHousekeeping(data.id, Housekeeping.DIRTY)) },
        )
        else -> if (data.active && data.occupancy == Occupancy.OCCUPIED) {
            Text(text = stringResource(Res.string.rooms_occupied_hint), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
        }
    }
}

@Composable
private fun MarkButton(text: String, hint: String, style: ButtonStyle, marking: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        MajesticButton(
            text = text,
            onClick = onClick,
            style = style,
            size = ButtonSize.Large,
            icon = if (style == ButtonStyle.Primary) MajesticIcons.checkCircle else MajesticIcons.cleaning,
            loading = marking,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = hint, style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
    }
}

/** "Doble · 2 personas · Piso 2". */
@Composable
private fun BoardRoom.summary(): String =
    listOfNotNull(typeName(), type?.let { capacityLabel(it.capacity) }, stringResource(Res.string.rooms_floor, room.floor))
        .joinToString(" · ")

@Composable
private fun BoardRoom.lastChangeLabel(): String? {
    val at = room.housekeepingChangedAt ?: return null
    val time = formatBogotaDateTime(at)
    return housekeepingChangedByName?.let { name -> stringResource(Res.string.rooms_last_change, name, time) }
        ?: stringResource(Res.string.rooms_last_change_without_name, time)
}

@Composable
internal fun RoomFormSheet(form: RoomForm, state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    val editedNumber = form.roomId?.let(state.board::roomOf)?.room?.number
    TaskSheet(
        title = editedNumber?.let { stringResource(Res.string.rooms_form_edit_title, it) } ?: stringResource(Res.string.rooms_add),
        onDismiss = { onIntent(RoomsIntent.DismissForm) },
        asDialog = LocalWideWindow.current,
    ) {
        RoomFormFields(form = form, state = state, onIntent = onIntent)
    }
}

/** RF-HAB-02: the number, the floor and the type. Only an existing room is turned off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomFormFields(form: RoomForm, state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val numberError = form.error.takeIf { it == RoomError.InvalidNumber || it == RoomError.DuplicateNumber }
    val floorError = form.error.takeIf { it == RoomError.InvalidFloor }
    Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
        MajesticTextField(
            value = form.numberText,
            onValueChange = { onIntent(RoomsIntent.NumberChange(it)) },
            label = stringResource(Res.string.rooms_form_number),
            error = numberError?.let { stringResource(it.message()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        MajesticTextField(
            value = form.floorText,
            onValueChange = { onIntent(RoomsIntent.FloorChange(it)) },
            label = stringResource(Res.string.rooms_form_floor),
            error = floorError?.let { stringResource(it.message()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = stringResource(Res.string.rooms_form_type))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            state.selectableTypes.forEach { type ->
                MajesticChip(text = type.name, selected = form.roomTypeId == type.id, onClick = { onIntent(RoomsIntent.TypeSelect(type.id)) })
            }
        }
        if (form.error == RoomError.MissingType) {
            Text(text = stringResource(Res.string.room_error_missing_type), style = typography.caption, color = colors.danger)
        }
    }
    if (form.roomId == null) {
        Text(text = stringResource(Res.string.rooms_form_new_hint), style = typography.caption, color = colors.inkMuted)
    } else {
        MajesticSwitchRow(
            title = stringResource(Res.string.rooms_form_active),
            help = stringResource(Res.string.rooms_form_active_hint),
            checked = form.active,
            onCheckedChange = { onIntent(RoomsIntent.ActiveChange(it)) },
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End)) {
        MajesticButton(text = stringResource(Res.string.cancel), onClick = { onIntent(RoomsIntent.DismissForm) }, style = ButtonStyle.Text)
        MajesticButton(text = stringResource(Res.string.save), onClick = { onIntent(RoomsIntent.SaveRoom) }, enabled = !state.isSaving, loading = state.isSaving)
    }
}
