package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEvent
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSnapshot
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** What a reservation was or became, inside an event of its history. */
@Serializable
internal data class ReservationSnapshotDto(
    val checkInDate: String = "",
    val checkOutDate: String = "",
    val roomNumber: Int = 0,
    val adults: Int = 0,
    val children: Int = 0,
    val nightlyRate: Long = 0,
    val total: Long = 0,
    val source: String = "",
    val notes: String = "",
)

/** An event of the history at `Reservations/{id}/events/{id}` (decision 1A), signed with the time of the server. */
@Serializable
internal data class ReservationEventDto(
    val type: String = "",
    val by: String = "",
    val at: BaseTimestamp? = null,
    val reason: String? = null,
    val before: ReservationSnapshotDto? = null,
    val after: ReservationSnapshotDto? = null,
)

internal fun ReservationEvent.toNewDto() = ReservationEventDto(
    type = type.code(),
    by = by,
    at = Timestamp.ServerTimestamp,
    reason = reason,
    before = before?.toDto(),
    after = after?.toDto(),
)

/** Null when its type is unknown, so a history never shows a change the app cannot explain. */
internal fun ReservationEventDto.toEvent(id: String): ReservationEvent? {
    val eventType = ReservationEventType.entries.firstOrNull { it.code() == type } ?: return null
    return ReservationEvent(
        id = id,
        type = eventType,
        by = by,
        at = (at as? Timestamp)?.toInstant(),
        reason = reason,
        before = before?.toSnapshot(),
        after = after?.toSnapshot(),
    )
}

internal fun ReservationEventType.code(): String = when (this) {
    ReservationEventType.MODIFIED -> "modified"
    ReservationEventType.CANCELLED -> "cancelled"
    ReservationEventType.NO_SHOW -> "no_show"
}

private fun ReservationSnapshot.toDto() = ReservationSnapshotDto(
    checkInDate = stay.checkIn.isoText,
    checkOutDate = stay.checkOut.isoText,
    roomNumber = roomNumber,
    adults = adults,
    children = children,
    nightlyRate = nightlyRate,
    total = total,
    source = source.code(),
    notes = notes,
)

private fun ReservationSnapshotDto.toSnapshot(): ReservationSnapshot? {
    val checkIn = CalendarDate.parse(checkInDate) ?: return null
    val checkOut = CalendarDate.parse(checkOutDate) ?: return null
    return ReservationSnapshot(
        stay = Stay(checkIn, checkOut),
        roomNumber = roomNumber,
        adults = adults,
        children = children,
        nightlyRate = nightlyRate,
        total = total,
        source = ReservationSource.entries.firstOrNull { it.code() == source } ?: ReservationSource.DIRECT,
        notes = notes,
    )
}
