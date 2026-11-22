package com.peakdevcol.project.hotelmanagement.ui.rooms

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.BoardRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Occupancy
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomStatus
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.room_capacity
import com.peakdevcol.project.hotelmanagement.resources.room_error_blank_name
import com.peakdevcol.project.hotelmanagement.resources.room_error_capacity
import com.peakdevcol.project.hotelmanagement.resources.room_error_changed
import com.peakdevcol.project.hotelmanagement.resources.room_error_duplicate_number
import com.peakdevcol.project.hotelmanagement.resources.room_error_duplicate_type
import com.peakdevcol.project.hotelmanagement.resources.room_error_floor
import com.peakdevcol.project.hotelmanagement.resources.room_error_missing_type
import com.peakdevcol.project.hotelmanagement.resources.room_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.room_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.room_error_not_found
import com.peakdevcol.project.hotelmanagement.resources.room_error_number
import com.peakdevcol.project.hotelmanagement.resources.room_error_rate
import com.peakdevcol.project.hotelmanagement.resources.room_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.rooms_check_out
import com.peakdevcol.project.hotelmanagement.resources.rooms_filter_available
import com.peakdevcol.project.hotelmanagement.resources.rooms_filter_cleaning
import com.peakdevcol.project.hotelmanagement.resources.rooms_filter_dirty
import com.peakdevcol.project.hotelmanagement.resources.rooms_filter_occupied
import com.peakdevcol.project.hotelmanagement.resources.rooms_type_missing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

fun RoomError.message(): StringResource = when (this) {
    RoomError.BlankName -> Res.string.room_error_blank_name
    RoomError.DuplicateTypeName -> Res.string.room_error_duplicate_type
    RoomError.InvalidCapacity -> Res.string.room_error_capacity
    RoomError.InvalidRate -> Res.string.room_error_rate
    RoomError.InvalidNumber -> Res.string.room_error_number
    RoomError.DuplicateNumber -> Res.string.room_error_duplicate_number
    RoomError.InvalidFloor -> Res.string.room_error_floor
    RoomError.MissingType -> Res.string.room_error_missing_type
    RoomError.Changed -> Res.string.room_error_changed
    RoomError.NotFound -> Res.string.room_error_not_found
    RoomError.NoSession -> Res.string.room_error_no_session
    RoomError.NotAllowed -> Res.string.room_error_not_allowed
    RoomError.Unknown -> Res.string.room_error_unknown
}

/** The name of a state in the filters of the board, in plural: "Disponibles". */
fun RoomStatus.filterLabel(): StringResource = when (this) {
    RoomStatus.AVAILABLE -> Res.string.rooms_filter_available
    RoomStatus.OCCUPIED -> Res.string.rooms_filter_occupied
    RoomStatus.DIRTY -> Res.string.rooms_filter_dirty
    RoomStatus.CLEANING -> Res.string.rooms_filter_cleaning
}

/** How many people sleep in a type: "2 personas". */
@Composable
fun capacityLabel(capacity: Int): String = pluralStringResource(Res.plurals.room_capacity, capacity, capacity)

@Composable
fun BoardRoom.typeName(): String = type?.name ?: stringResource(Res.string.rooms_type_missing)

/** RF-HAB-04: when an occupied room is expected to be left, as the board shows it: "Sale el dom 22 nov". */
@Composable
fun BoardRoom.checkOutLabel(): String? =
    room.expectedCheckOutDate
        ?.takeIf { room.occupancy == Occupancy.OCCUPIED }
        ?.let { stringResource(Res.string.rooms_check_out, formatWeekday(it)) }
