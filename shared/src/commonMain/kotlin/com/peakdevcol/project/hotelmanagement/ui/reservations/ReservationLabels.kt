package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.document_type_cc
import com.peakdevcol.project.hotelmanagement.resources.document_type_ce
import com.peakdevcol.project.hotelmanagement.resources.document_type_other
import com.peakdevcol.project.hotelmanagement.resources.document_type_passport
import com.peakdevcol.project.hotelmanagement.resources.document_type_ppt
import com.peakdevcol.project.hotelmanagement.resources.document_type_ti
import com.peakdevcol.project.hotelmanagement.resources.guest_error_blank_name
import com.peakdevcol.project.hotelmanagement.resources.guest_error_duplicate_document
import com.peakdevcol.project.hotelmanagement.resources.guest_error_email
import com.peakdevcol.project.hotelmanagement.resources.guest_error_incomplete_document
import com.peakdevcol.project.hotelmanagement.resources.guest_error_no_connection
import com.peakdevcol.project.hotelmanagement.resources.guest_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.guest_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.guest_error_phone
import com.peakdevcol.project.hotelmanagement.resources.guest_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest_without_document
import com.peakdevcol.project.hotelmanagement.resources.reservation_adults
import com.peakdevcol.project.hotelmanagement.resources.reservation_children
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_dates
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_missing_guest
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_missing_room
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_nights_taken
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_no_adults
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_no_connection
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_over_capacity
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_past
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_rate
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_rate_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_too_many_nights
import com.peakdevcol.project.hotelmanagement.resources.reservation_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.reservation_nights
import com.peakdevcol.project.hotelmanagement.resources.reservation_source_agency
import com.peakdevcol.project.hotelmanagement.resources.reservation_source_direct
import com.peakdevcol.project.hotelmanagement.resources.reservation_source_phone
import com.peakdevcol.project.hotelmanagement.resources.reservation_source_whatsapp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

fun GuestError.message(): StringResource = when (this) {
    GuestError.BlankName -> Res.string.guest_error_blank_name
    GuestError.InvalidPhone -> Res.string.guest_error_phone
    GuestError.IncompleteDocument -> Res.string.guest_error_incomplete_document
    GuestError.DuplicateDocument -> Res.string.guest_error_duplicate_document
    GuestError.InvalidEmail -> Res.string.guest_error_email
    GuestError.NoSession -> Res.string.guest_error_no_session
    GuestError.NotAllowed -> Res.string.guest_error_not_allowed
    GuestError.NoConnection -> Res.string.guest_error_no_connection
    GuestError.Unknown -> Res.string.guest_error_unknown
}

/** The message of a reservation that was not made. The nights taken are named by their day. */
@Composable
fun ReservationError.text(): String = when (this) {
    ReservationError.CheckOutNotAfterCheckIn -> stringResource(Res.string.reservation_error_dates)
    ReservationError.TooManyNights -> stringResource(Res.string.reservation_error_too_many_nights)
    ReservationError.ArrivalInThePast -> stringResource(Res.string.reservation_error_past)
    ReservationError.NoAdults -> stringResource(Res.string.reservation_error_no_adults)
    ReservationError.OverCapacity -> stringResource(Res.string.reservation_error_over_capacity)
    ReservationError.MissingGuest -> stringResource(Res.string.reservation_error_missing_guest)
    ReservationError.MissingRoom -> stringResource(Res.string.reservation_error_missing_room)
    ReservationError.InvalidRate -> stringResource(Res.string.reservation_error_rate)
    ReservationError.RateNotAllowed -> stringResource(Res.string.reservation_error_rate_not_allowed)
    is ReservationError.NightsTaken -> stringResource(Res.string.reservation_error_nights_taken, nights.joinToString(", ") { formatWeekday(it) })
    ReservationError.NoSession -> stringResource(Res.string.reservation_error_no_session)
    ReservationError.NotAllowed -> stringResource(Res.string.reservation_error_not_allowed)
    ReservationError.NoConnection -> stringResource(Res.string.reservation_error_no_connection)
    ReservationError.Unknown -> stringResource(Res.string.reservation_error_unknown)
}

fun ReservationSource.label(): StringResource = when (this) {
    ReservationSource.DIRECT -> Res.string.reservation_source_direct
    ReservationSource.PHONE -> Res.string.reservation_source_phone
    ReservationSource.WHATSAPP -> Res.string.reservation_source_whatsapp
    ReservationSource.AGENCY -> Res.string.reservation_source_agency
}

fun DocumentType.label(): StringResource = when (this) {
    DocumentType.CC -> Res.string.document_type_cc
    DocumentType.CE -> Res.string.document_type_ce
    DocumentType.PASSPORT -> Res.string.document_type_passport
    DocumentType.TI -> Res.string.document_type_ti
    DocumentType.PPT -> Res.string.document_type_ppt
    DocumentType.OTHER -> Res.string.document_type_other
}

/** "2 noches". */
@Composable
fun nightsLabel(nights: Int): String = pluralStringResource(Res.plurals.reservation_nights, nights, nights)

/** "2 adultos · 1 niño", or only the adults when there are no children. */
@Composable
fun peopleLabel(adults: Int, children: Int): String {
    val adultsText = pluralStringResource(Res.plurals.reservation_adults, adults, adults)
    return if (children == 0) adultsText else "$adultsText · ${pluralStringResource(Res.plurals.reservation_children, children, children)}"
}

/** "CC 1020304 · 300 123 4567", or "Sin documento · 300 123 4567" until it is given (decision 5A). */
@Composable
fun Guest.identityLabel(): String {
    val type = documentType
    val number = documentNumber
    val document = if (type != null && number != null) "${stringResource(type.label())} $number" else stringResource(Res.string.new_reservation_guest_without_document)
    return "$document · $phone"
}
