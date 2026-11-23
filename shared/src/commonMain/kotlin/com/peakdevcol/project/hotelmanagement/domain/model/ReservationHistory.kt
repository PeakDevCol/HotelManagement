package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/** What a reservation was like before or after a change, for its history (decision 1A, RNF-11). */
data class ReservationSnapshot(
    val stay: Stay,
    val roomNumber: Int,
    val adults: Int,
    val children: Int,
    val nightlyRate: Long,
    val total: Long,
    val source: ReservationSource,
    val notes: String,
)

fun Reservation.snapshot() = ReservationSnapshot(
    stay = stay,
    roomNumber = roomNumber,
    adults = adults,
    children = children,
    nightlyRate = nightlyRates.firstOrNull() ?: 0,
    total = total,
    source = source,
    notes = notes,
)

enum class ReservationEventType {
    /** RF-RES-05. */
    MODIFIED,

    /** RF-RES-06. */
    CANCELLED,

    /** RF-RES-07. */
    NO_SHOW,
}

/**
 * One change of a reservation, signed with who made it and the time of the server. The creation is
 * not an event: the reservation itself says who created it and when.
 */
data class ReservationEvent(
    val id: String,
    val type: ReservationEventType,
    val by: String,
    val at: Instant? = null,
    /** Why it was cancelled (RF-RES-06). */
    val reason: String? = null,
    /** What the reservation was and became, when it was modified. */
    val before: ReservationSnapshot? = null,
    val after: ReservationSnapshot? = null,
)

/** An event of the history with the name of who made it, null when the employee is unknown. */
data class ReservationHistoryEntry(val event: ReservationEvent, val byName: String?)

/** RF-RES-09: an arrival of today, with how its room is now. */
data class TodayArrival(val reservation: Reservation, val roomStatus: RoomStatus?)

/**
 * RF-RES-07 and RF-RES-09: what reception has to do today. [overdue] are the confirmed reservations
 * whose day of arrival is over, to be marked as not arrived.
 */
data class TodayReservations(
    val today: CalendarDate,
    val arrivals: List<TodayArrival> = emptyList(),
    val departures: List<Reservation> = emptyList(),
    val overdue: List<Reservation> = emptyList(),
) {
    val isEmpty: Boolean get() = arrivals.isEmpty() && departures.isEmpty() && overdue.isEmpty()

    fun reservationOf(reservationId: String): Reservation? =
        (arrivals.map { it.reservation } + departures + overdue).firstOrNull { it.id == reservationId }
}
