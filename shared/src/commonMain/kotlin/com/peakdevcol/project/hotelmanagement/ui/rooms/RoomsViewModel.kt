package com.peakdevcol.project.hotelmanagement.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.RoomBoard
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomFloor
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.byFloor
import com.peakdevcol.project.hotelmanagement.domain.usecase.MarkHousekeepingUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveRoomBoardUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveRoomUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The room being created, or edited when [roomId] is not null (RF-HAB-02, decision 1A). */
data class RoomForm(
    val roomId: String? = null,
    val numberText: String = "",
    val floorText: String = "",
    val roomTypeId: String? = null,
    val active: Boolean = true,
    /** The field the last attempt to save found wrong. */
    val error: RoomError? = null,
)

data class RoomsState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val board: RoomBoard = RoomBoard(),
    /** RF-HAB-04: only the rooms in this state, or every room when null. */
    val filter: RoomStatus? = null,
    /** The room whose detail is open. */
    val openRoomId: String? = null,
    val form: RoomForm? = null,
    val isSaving: Boolean = false,
    /** The room being marked, so its button waits for the answer and is not touched twice. */
    val markingRoomId: String? = null,
) {
    val floors: List<RoomFloor> get() = board.rooms.filter { filter == null || it.status == filter }.byFloor()

    val openRoom: BoardRoom? get() = openRoomId?.let(board::roomOf)

    /** A room needs a type, so the first step of an administrator without active types is to create one. */
    val needsTypes: Boolean get() = board.canManage && board.types.none { it.active }

    val canAddRooms: Boolean get() = board.canManage && !needsTypes

    /** How many rooms a filter chip would show: every active room for a null [status]. */
    fun countOf(status: RoomStatus?): Int = board.rooms.count { status == null || it.status == status }

    /** The types the form offers: the active ones, and the one the room already has. */
    val selectableTypes: List<RoomType>
        get() {
            val currentTypeId = form?.roomId?.let(board::roomOf)?.room?.roomTypeId
            return board.types.filter { it.active || it.id == currentTypeId }
        }
}

sealed interface RoomsIntent {
    /** Choosing the state already chosen, or "Todas" with a null [status], shows every room again. */
    data class SelectFilter(val status: RoomStatus?) : RoomsIntent
    data class OpenRoom(val roomId: String) : RoomsIntent
    data object CloseRoom : RoomsIntent

    /** RF-HAB-03 with decision 5A. */
    data class MarkHousekeeping(val roomId: String, val housekeeping: Housekeeping) : RoomsIntent
    data object ManageTypes : RoomsIntent
    data object AddRoom : RoomsIntent
    data class EditRoom(val roomId: String) : RoomsIntent
    data class NumberChange(val text: String) : RoomsIntent
    data class FloorChange(val text: String) : RoomsIntent
    data class TypeSelect(val roomTypeId: String) : RoomsIntent
    data class ActiveChange(val active: Boolean) : RoomsIntent
    data object SaveRoom : RoomsIntent
    data object DismissForm : RoomsIntent
    data object Retry : RoomsIntent
}

sealed interface RoomsEffect {
    data object NavigateToRoomTypes : RoomsEffect
    data class Marked(val number: Int, val housekeeping: Housekeeping) : RoomsEffect
    data class Failed(val error: RoomError) : RoomsEffect
}

/** RF-HAB-02 to RF-HAB-04: the board of every employee, where an administrator also sets the rooms up. */
class RoomsViewModel(
    private val observeRoomBoard: ObserveRoomBoardUseCase,
    private val markHousekeeping: MarkHousekeepingUseCase,
    private val saveRoom: SaveRoomUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomsState())
    val state: StateFlow<RoomsState> = _state.asStateFlow()

    private val _effects = Channel<RoomsEffect>(Channel.BUFFERED)
    val effects: Flow<RoomsEffect> = _effects.receiveAsFlow()

    private var boardJob: Job? = null

    init {
        listenToBoard()
    }

    fun onIntent(intent: RoomsIntent) {
        when (intent) {
            is RoomsIntent.SelectFilter -> _state.update { it.copy(filter = intent.status.takeIf { status -> status != it.filter }) }
            is RoomsIntent.OpenRoom -> _state.update { it.copy(openRoomId = intent.roomId) }
            RoomsIntent.CloseRoom -> _state.update { it.copy(openRoomId = null) }
            is RoomsIntent.MarkHousekeeping -> mark(intent.roomId, intent.housekeeping)
            RoomsIntent.ManageTypes -> _effects.trySend(RoomsEffect.NavigateToRoomTypes)
            RoomsIntent.AddRoom -> addRoom()
            is RoomsIntent.EditRoom -> editRoom(intent.roomId)
            is RoomsIntent.NumberChange -> editForm { it.copy(numberText = intent.text.filter(Char::isDigit).take(MAX_NUMBER_DIGITS)) }
            is RoomsIntent.FloorChange -> editForm { it.copy(floorText = intent.text.filter(Char::isDigit).take(MAX_FLOOR_DIGITS)) }
            is RoomsIntent.TypeSelect -> editForm { it.copy(roomTypeId = intent.roomTypeId) }
            is RoomsIntent.ActiveChange -> editForm { it.copy(active = intent.active) }
            RoomsIntent.SaveRoom -> save()
            RoomsIntent.DismissForm -> _state.update { it.copy(form = null) }
            RoomsIntent.Retry -> listenToBoard()
        }
    }

    private fun listenToBoard() {
        boardJob?.cancel()
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        boardJob = viewModelScope.launch {
            observeRoomBoard()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { board -> _state.update { it.copy(isLoading = false, board = board) } }
        }
    }

    // The detail closes once marked, so a second touch never undoes what the first one did.
    private fun mark(roomId: String, housekeeping: Housekeeping) {
        val current = _state.value
        if (current.markingRoomId != null) return
        val room = current.board.roomOf(roomId)?.room ?: return
        _state.update { it.copy(markingRoomId = roomId) }
        viewModelScope.launch {
            val error = markHousekeeping(room, housekeeping).exceptionOrNull()?.let { it as? RoomError ?: RoomError.Unknown }
            _state.update { state ->
                state.copy(markingRoomId = null, openRoomId = state.openRoomId.takeUnless { error == null && it == roomId })
            }
            _effects.send(if (error == null) RoomsEffect.Marked(room.number, housekeeping) else RoomsEffect.Failed(error))
        }
    }

    private fun addRoom() {
        val state = _state.value
        if (!state.canAddRooms) return
        _state.update { it.copy(form = RoomForm(roomTypeId = state.board.types.singleOrNull { type -> type.active }?.id)) }
    }

    private fun editRoom(roomId: String) {
        val state = _state.value
        val room = state.board.roomOf(roomId)?.room ?: return
        if (!state.board.canManage) return
        val form = RoomForm(
            roomId = room.id,
            numberText = room.number.toString(),
            floorText = room.floor.toString(),
            roomTypeId = room.roomTypeId.takeIf { typeId -> state.board.types.any { it.id == typeId } },
            active = room.active,
        )
        _state.update { it.copy(openRoomId = null, form = form) }
    }

    private fun editForm(change: (RoomForm) -> RoomForm) = _state.update { state ->
        state.copy(form = state.form?.let { change(it).copy(error = null) })
    }

    // A wrong field keeps the form open with its error; anything else closes it.
    private fun save() {
        val form = _state.value.form ?: return
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val error = saveRoom(form.roomId, form.numberText.toIntOrNull(), form.floorText.toIntOrNull(), form.roomTypeId, form.active)
                .exceptionOrNull()
                ?.let { it as? RoomError ?: RoomError.Unknown }
            val fieldError = error.takeIf { it in FIELD_ERRORS }
            _state.update { it.copy(isSaving = false, form = fieldError?.let { form.copy(error = fieldError) }) }
            if (error != null && fieldError == null) _effects.send(RoomsEffect.Failed(error))
        }
    }

    private companion object {
        const val MAX_NUMBER_DIGITS = 4
        const val MAX_FLOOR_DIGITS = 2
        val FIELD_ERRORS = setOf(RoomError.InvalidNumber, RoomError.DuplicateNumber, RoomError.InvalidFloor, RoomError.MissingType)
    }
}
