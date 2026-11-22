package com.peakdevcol.project.hotelmanagement.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.nav_rooms
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.resources.rooms_add
import com.peakdevcol.project.hotelmanagement.resources.rooms_create_type
import com.peakdevcol.project.hotelmanagement.resources.rooms_empty_admin_message
import com.peakdevcol.project.hotelmanagement.resources.rooms_empty_filter_message
import com.peakdevcol.project.hotelmanagement.resources.rooms_empty_filter_title
import com.peakdevcol.project.hotelmanagement.resources.rooms_empty_message
import com.peakdevcol.project.hotelmanagement.resources.rooms_empty_title
import com.peakdevcol.project.hotelmanagement.resources.rooms_error
import com.peakdevcol.project.hotelmanagement.resources.rooms_filter_all
import com.peakdevcol.project.hotelmanagement.resources.rooms_floor
import com.peakdevcol.project.hotelmanagement.resources.rooms_inactive_title
import com.peakdevcol.project.hotelmanagement.resources.rooms_marked_clean
import com.peakdevcol.project.hotelmanagement.resources.rooms_marked_dirty
import com.peakdevcol.project.hotelmanagement.resources.rooms_no_types_message
import com.peakdevcol.project.hotelmanagement.resources.rooms_no_types_title
import com.peakdevcol.project.hotelmanagement.resources.rooms_types
import com.peakdevcol.project.hotelmanagement.resources.rooms_types_hint
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomTile
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomTileMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ScrollingChipRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * RF-HAB-04: the board of the rooms, live, by floor. [onBack] is the arrow back to "Más", when the
 * section was opened from there on the phone.
 */
@Composable
fun RoomsScreen(onTypesClick: () -> Unit, onBack: (() -> Unit)? = null, viewModel: RoomsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoomsEffect.NavigateToRoomTypes -> onTypesClick()
                is RoomsEffect.Marked -> {
                    val message = if (effect.housekeeping == Housekeeping.CLEAN) Res.string.rooms_marked_clean else Res.string.rooms_marked_dirty
                    messages.show(getString(message, effect.number))
                }
                is RoomsEffect.Failed -> messages.show(getString(effect.error.message()))
            }
        }
    }

    RoomsContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun RoomsContent(state: RoomsState, onIntent: (RoomsIntent) -> Unit, onBack: (() -> Unit)? = null) {
    val wide = LocalWideWindow.current
    Scaffold(
        topBar = {
            MajesticTopBar(title = stringResource(Res.string.nav_rooms), onBack = onBack) {
                if (wide && state.canAddRooms) {
                    MajesticButton(
                        text = stringResource(Res.string.rooms_add),
                        onClick = { onIntent(RoomsIntent.AddRoom) },
                        icon = MajesticIcons.add,
                        modifier = Modifier.padding(end = MajesticSpacing.space3),
                    )
                }
            }
        },
        floatingActionButton = {
            if (!wide && state.canAddRooms) {
                MajesticFloatingActionButton(text = stringResource(Res.string.rooms_add), icon = MajesticIcons.add, onClick = { onIntent(RoomsIntent.AddRoom) })
            }
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        RoomsBoard(
            state = state,
            onIntent = onIntent,
            // Room for the floating button, so it never covers the last room.
            bottomClearance = if (!wide && state.canAddRooms) MajesticSizes.floatingActionButtonClearance else MajesticSpacing.space4,
            modifier = Modifier.padding(padding),
        )
    }
    state.openRoom?.let { room -> RoomDetailSheet(room = room, state = state, onIntent = onIntent) }
    state.form?.let { form -> RoomFormSheet(form = form, state = state, onIntent = onIntent) }
}

@Composable
private fun RoomsBoard(state: RoomsState, onIntent: (RoomsIntent) -> Unit, bottomClearance: Dp, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> MajesticLoading(modifier)
        state.loadFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.rooms_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(RoomsIntent.Retry) },
            modifier = modifier,
        )
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(RoomTileMinWidth),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MajesticSpacing.space4, end = MajesticSpacing.space4, top = MajesticSpacing.space2, bottom = bottomClearance),
            horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        ) {
            if (state.board.canManage) wholeRow("setup") { RoomSetup(onIntent) }
            if (state.board.rooms.isEmpty()) {
                wholeRow("empty") { EmptyBoard(state, onIntent) }
            } else {
                wholeRow("filters") { RoomFilters(state, onIntent) }
                activeRooms(state, onIntent)
            }
            inactiveRooms(state, onIntent)
        }
    }
}

private fun LazyGridScope.wholeRow(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.activeRooms(state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    val floors = state.floors
    if (floors.isEmpty()) {
        wholeRow("empty-filter") {
            EmptyState(
                icon = MajesticIcons.filter,
                title = stringResource(Res.string.rooms_empty_filter_title),
                message = stringResource(Res.string.rooms_empty_filter_message),
            )
        }
    }
    // Prefixed, so a room id never takes the key of a heading.
    floors.forEach { floor ->
        wholeRow("floor-${floor.floor}") { FloorTitle(stringResource(Res.string.rooms_floor, floor.floor)) }
        items(floor.rooms, key = { "room-${it.room.id}" }) { room -> BoardRoomTile(room = room, onIntent = onIntent) }
    }
}

/** Only administrators get the inactive rooms, to turn them on again. They are not part of any state. */
private fun LazyGridScope.inactiveRooms(state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    val inactive = state.board.inactiveRooms
    if (inactive.isEmpty() || state.filter != null) return
    wholeRow("inactive") { FloorTitle(stringResource(Res.string.rooms_inactive_title)) }
    items(inactive, key = { "inactive-${it.room.id}" }) { room -> BoardRoomTile(room = room, onIntent = onIntent) }
}

@Composable
private fun FloorTitle(text: String) {
    SectionLabel(text = text, modifier = Modifier.padding(top = MajesticSpacing.space2))
}

@Composable
private fun BoardRoomTile(room: BoardRoom, onIntent: (RoomsIntent) -> Unit) {
    RoomTile(
        number = room.room.number,
        typeName = room.typeName(),
        status = room.status,
        detail = room.checkOutLabel(),
        inactive = !room.room.active,
        onClick = { onIntent(RoomsIntent.OpenRoom(room.room.id)) },
    )
}

/** RF-HAB-01, with decision 1A: the types are one touch away from the board, like the taxes from the inventory. */
@Composable
private fun RoomSetup(onIntent: (RoomsIntent) -> Unit) {
    ListGroup {
        ListRow(
            title = stringResource(Res.string.rooms_types),
            subtitle = stringResource(Res.string.rooms_types_hint),
            leading = { MajesticIcon(icon = MajesticIcons.category, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
            trailing = { MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
            onClick = { onIntent(RoomsIntent.ManageTypes) },
        )
    }
}

/** RF-HAB-04: each chip says how many rooms are in that state. */
@Composable
private fun RoomFilters(state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    ScrollingChipRow {
        MajesticChip(
            text = stringResource(Res.string.rooms_filter_all),
            selected = state.filter == null,
            count = state.countOf(null),
            onClick = { onIntent(RoomsIntent.SelectFilter(null)) },
        )
        RoomStatus.entries.forEach { status ->
            MajesticChip(
                text = stringResource(status.filterLabel()),
                selected = state.filter == status,
                count = state.countOf(status),
                onClick = { onIntent(RoomsIntent.SelectFilter(status)) },
            )
        }
    }
}

@Composable
private fun EmptyBoard(state: RoomsState, onIntent: (RoomsIntent) -> Unit) {
    when {
        state.needsTypes -> EmptyState(
            icon = MajesticIcons.category,
            title = stringResource(Res.string.rooms_no_types_title),
            message = stringResource(Res.string.rooms_no_types_message),
            actionText = stringResource(Res.string.rooms_create_type),
            actionIcon = MajesticIcons.add,
            onAction = { onIntent(RoomsIntent.ManageTypes) },
        )
        state.board.canManage -> EmptyState(
            icon = MajesticIcons.room,
            title = stringResource(Res.string.rooms_empty_title),
            message = stringResource(Res.string.rooms_empty_admin_message),
            actionText = stringResource(Res.string.rooms_add),
            actionIcon = MajesticIcons.add,
            onAction = { onIntent(RoomsIntent.AddRoom) },
        )
        else -> EmptyState(
            icon = MajesticIcons.room,
            title = stringResource(Res.string.rooms_empty_title),
            message = stringResource(Res.string.rooms_empty_message),
        )
    }
}
