package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.formatThousands
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_already_open
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_device_not_authorized
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_imprecise
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_inactive_account
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_invalid_credentials
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_invalid_name
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_location_disabled
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_location_unavailable
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_network
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_no_open_entry
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_no_zone
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_outside
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_permission_denied
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_simulated
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_timed_out
import com.peakdevcol.project.hotelmanagement.resources.attendance_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** What went wrong, and what to do instead: most of the time, marking at reception (RF-ASI-03). */
@Composable
fun AttendanceError.text(): String = when (this) {
    is AttendanceError.OutsideZone -> stringResource(Res.string.attendance_error_outside, formatThousands(distanceMeters.toLong()))
    is AttendanceError.ImpreciseLocation -> stringResource(Res.string.attendance_error_imprecise, formatThousands(accuracyMeters.toLong()))
    else -> stringResource(messageRes())
}

suspend fun AttendanceError.message(): String = when (this) {
    is AttendanceError.OutsideZone -> getString(Res.string.attendance_error_outside, formatThousands(distanceMeters.toLong()))
    is AttendanceError.ImpreciseLocation -> getString(Res.string.attendance_error_imprecise, formatThousands(accuracyMeters.toLong()))
    else -> getString(messageRes())
}

private fun AttendanceError.messageRes(): StringResource = when (this) {
    AttendanceError.NoSession -> Res.string.order_error_no_session
    AttendanceError.NoZone -> Res.string.attendance_error_no_zone
    AttendanceError.LocationPermissionDenied -> Res.string.attendance_error_permission_denied
    AttendanceError.LocationDisabled -> Res.string.attendance_error_location_disabled
    AttendanceError.LocationUnavailable -> Res.string.attendance_error_location_unavailable
    AttendanceError.LocationTimedOut -> Res.string.attendance_error_timed_out
    AttendanceError.SimulatedLocation -> Res.string.attendance_error_simulated
    is AttendanceError.OutsideZone -> Res.string.attendance_error_outside
    is AttendanceError.ImpreciseLocation -> Res.string.attendance_error_imprecise
    AttendanceError.AlreadyOpen -> Res.string.attendance_error_already_open
    AttendanceError.NoOpenEntry -> Res.string.attendance_error_no_open_entry
    AttendanceError.InvalidCredentials -> Res.string.attendance_error_invalid_credentials
    AttendanceError.InactiveAccount -> Res.string.attendance_error_inactive_account
    AttendanceError.DeviceNotAuthorized -> Res.string.attendance_error_device_not_authorized
    AttendanceError.InvalidName -> Res.string.attendance_error_invalid_name
    AttendanceError.NotAllowed -> Res.string.attendance_error_not_allowed
    AttendanceError.Network -> Res.string.attendance_error_network
    AttendanceError.Unknown -> Res.string.attendance_error_unknown
}
