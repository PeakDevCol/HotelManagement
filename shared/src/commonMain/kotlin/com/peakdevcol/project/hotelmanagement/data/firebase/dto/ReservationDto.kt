package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.searchKeys
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * A guest stored at `Guests/{id}` (RF-RES-01). [searchKeys] lets Firestore find them by the
 * beginning of a word of the name or by the document, since it has no text search.
 */
@Serializable
internal data class GuestDto(
    val fullName: String = "",
    val phone: String = "",
    val documentType: String? = null,
    val documentNumber: String? = null,
    val nationality: String? = null,
    val email: String? = null,
    val dataConsentAt: BaseTimestamp? = null,
    val searchKeys: List<String> = emptyList(),
    val createdBy: String? = null,
    val createdAt: BaseTimestamp? = null,
    val updatedBy: String? = null,
    val updatedAt: BaseTimestamp? = null,
)

/** A new guest, signed by [createdBy] with the time of the server. */
internal fun Guest.toNewDto(createdBy: String) = GuestDto(
    fullName = fullName,
    phone = phone,
    documentType = documentType?.code(),
    documentNumber = documentNumber,
    nationality = nationality,
    email = email,
    searchKeys = searchKeys,
    createdBy = createdBy,
    createdAt = Timestamp.ServerTimestamp,
)

internal fun GuestDto.toGuest(id: String) = Guest(
    id = id,
    fullName = fullName,
    phone = phone,
    documentType = documentType?.let(::documentTypeOf),
    documentNumber = documentNumber,
    nationality = nationality,
    email = email,
    dataConsentAt = (dataConsentAt as? Timestamp)?.toInstant(),
)

internal fun DocumentType.code(): String = when (this) {
    DocumentType.CC -> "cc"
    DocumentType.CE -> "ce"
    DocumentType.PASSPORT -> "passport"
    DocumentType.TI -> "ti"
    DocumentType.PPT -> "ppt"
    DocumentType.OTHER -> "other"
}

private fun documentTypeOf(code: String): DocumentType = DocumentType.entries.firstOrNull { it.code() == code } ?: DocumentType.OTHER

/**
 * A reservation stored at `Reservations/{id}` (RF-RES-02). The days are days of Colombia, written
 * as `2026-11-22`, so they compare as text. [total] is stored for the rules, which cannot add up
 * a list.
 */
@Serializable
internal data class ReservationDto(
    val guestId: String = "",
    val guestName: String = "",
    val roomId: String = "",
    val roomNumber: Int = 0,
    val checkInDate: String = "",
    val checkOutDate: String = "",
    val adults: Int = 0,
    val children: Int = 0,
    val nightlyRates: List<Long> = emptyList(),
    val taxAmount: Long = 0,
    val total: Long = 0,
    val paidAmount: Long = 0,
    val source: String = "",
    val state: String = "",
    val notes: String = "",
    val createdBy: String? = null,
    val createdAt: BaseTimestamp? = null,
)

/** A new reservation, with the time of the server. */
internal fun Reservation.toNewDto() = ReservationDto(
    guestId = guestId,
    guestName = guestName,
    roomId = roomId,
    roomNumber = roomNumber,
    checkInDate = stay.checkIn.isoText,
    checkOutDate = stay.checkOut.isoText,
    adults = adults,
    children = children,
    nightlyRates = nightlyRates,
    taxAmount = taxAmount,
    total = total,
    paidAmount = paidAmount,
    source = source.code(),
    state = state.code(),
    notes = notes,
    createdBy = createdBy,
    createdAt = Timestamp.ServerTimestamp,
)

/** Null when its days cannot be read, so a broken document never shows wrong nights. */
internal fun ReservationDto.toReservation(id: String): Reservation? {
    val checkIn = CalendarDate.parse(checkInDate) ?: return null
    val checkOut = CalendarDate.parse(checkOutDate) ?: return null
    return Reservation(
        id = id,
        guestId = guestId,
        guestName = guestName,
        roomId = roomId,
        roomNumber = roomNumber,
        stay = Stay(checkIn, checkOut),
        adults = adults,
        children = children,
        nightlyRates = nightlyRates,
        taxAmount = taxAmount,
        source = ReservationSource.entries.firstOrNull { it.code() == source } ?: ReservationSource.DIRECT,
        state = ReservationState.entries.firstOrNull { it.code() == state } ?: ReservationState.CONFIRMED,
        notes = notes,
        paidAmount = paidAmount,
        createdBy = createdBy,
        createdAt = (createdAt as? Timestamp)?.toInstant(),
    )
}

internal fun ReservationState.code(): String = when (this) {
    ReservationState.CONFIRMED -> "confirmed"
    ReservationState.CHECKED_IN -> "checked_in"
    ReservationState.CHECKED_OUT -> "checked_out"
    ReservationState.CANCELLED -> "cancelled"
    ReservationState.NO_SHOW -> "no_show"
}

internal fun ReservationSource.code(): String = when (this) {
    ReservationSource.DIRECT -> "direct"
    ReservationSource.PHONE -> "phone"
    ReservationSource.WHATSAPP -> "whatsapp"
    ReservationSource.AGENCY -> "agency"
}

/** A night held at `RoomNights/{roomId}_{date}` (RF-RES-03). It carries no personal data. */
@Serializable
internal data class RoomNightDto(
    val roomId: String = "",
    val date: String = "",
    val reservationId: String = "",
    val arrival: Boolean = false,
)

internal fun RoomNight.toDto() = RoomNightDto(roomId = roomId, date = date.isoText, reservationId = reservationId, arrival = arrival)

internal fun RoomNightDto.toRoomNight(): RoomNight? =
    CalendarDate.parse(date)?.let { night -> RoomNight(roomId = roomId, date = night, reservationId = reservationId, arrival = arrival) }
