package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

/** The life of a reservation (section 6.9). */
enum class ReservationState {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    NO_SHOW,
}

/** RF-RES-02: how the reservation reached the hotel. */
enum class ReservationSource {
    DIRECT,
    PHONE,
    WHATSAPP,
    AGENCY,
}

/** Decision 4A of M11: a longer stay is made as two reservations. */
const val MAX_RESERVATION_NIGHTS = 30
const val MAX_RESERVATION_NOTES_LENGTH = 500

/**
 * The days of a stay, as days of Colombia: the guest arrives on [checkIn], which is the first
 * night, and leaves on the morning of [checkOut], so that day is not a night of the stay.
 */
data class Stay(val checkIn: CalendarDate, val checkOut: CalendarDate) {
    val nightCount: Int get() = (checkOut.epochDay - checkIn.epochDay).toInt()

    val nights: List<CalendarDate> get() = (0 until nightCount.coerceAtLeast(0)).map { checkIn.plusDays(it.toLong()) }
}

/**
 * RF-RES-02: one room for one guest between two days (P35). Each night keeps its own rate, so
 * seasons (RF-HAB-05) will not change the model. [guestName] and [roomNumber] are copies, so the
 * lists do not read the guest or the room.
 */
data class Reservation(
    val id: String,
    val guestId: String,
    val guestName: String,
    val roomId: String,
    val roomNumber: Int,
    val stay: Stay,
    val adults: Int,
    val children: Int,
    /** One rate per night, in pesos, with the tax included (decision 3A of M10). */
    val nightlyRates: List<Long>,
    /** The part of [total] that is tax. */
    val taxAmount: Long,
    val source: ReservationSource,
    val state: ReservationState = ReservationState.CONFIRMED,
    val notes: String = "",
    /** What the guest has paid. The payments of the stay come with the check-in (RF-EST-09, M12). */
    val paidAmount: Long = 0,
    val createdBy: String? = null,
    val createdAt: Instant? = null,
) {
    val total: Long get() = nightlyRates.sum()

    val people: Int get() = adults + children

    /** RF-RES-03: the nights this reservation holds in `RoomNights`, the first one marked as the arrival. */
    val roomNights: List<RoomNight>
        get() = stay.nights.map { night -> RoomNight(roomId = roomId, date = night, reservationId = id, arrival = night == stay.checkIn) }

    // S9: the name of the guest never reaches a log through the text of the object.
    override fun toString(): String = "Reservation(id=$id, room=$roomNumber, stay=$stay, state=$state)"
}

/**
 * RF-RES-03: one night of one room, held by a reservation. Its id is the room and the day, and
 * the rules only create it when it does not exist, so two reservations never share a night.
 */
data class RoomNight(
    val roomId: String,
    val date: CalendarDate,
    val reservationId: String,
    /** The first night of a reservation: housekeeping knows the arrivals of the day by it (RF-LIM-02). */
    val arrival: Boolean,
) {
    val id: String get() = "${roomId}_${date.isoText}"
}

/** RF-RES-03: a room free for every night of a stay, with what the stay costs in it. */
data class AvailableRoom(
    val room: Room,
    val type: RoomType,
    /** Each night at the base rate of the type, with the tax included. */
    val nightlyRate: Long,
    /** The rate of the tax of the type, if it is active, in hundredths of a point. */
    val taxRate: Int,
)

/** What the reservation would cost at [nightlyRate] for [nights] nights, and the tax it includes. */
data class StayPrice(val nightlyRate: Long, val nights: Int, val taxRate: Int) {
    val total: Long get() = nightlyRate * nights
    val taxAmount: Long get() = taxIncludedIn(total, taxRate)
}

/** Why a reservation was not made, or the free rooms were not found. */
sealed class ReservationError(message: String) : Exception(message) {
    data object CheckOutNotAfterCheckIn : ReservationError("The check-out must be after the check-in")
    data object TooManyNights : ReservationError("A reservation has at most $MAX_RESERVATION_NIGHTS nights")
    data object ArrivalInThePast : ReservationError("The arrival cannot be before today")
    data object NoAdults : ReservationError("At least one adult")
    data object OverCapacity : ReservationError("More people than the room takes")
    data object MissingGuest : ReservationError("The reservation needs a guest")
    data object MissingRoom : ReservationError("The reservation needs a free room")
    data object InvalidRate : ReservationError("The rate must be greater than zero")

    /** RF-RES-02: only an administrator changes the base rate of the type. */
    data object RateNotAllowed : ReservationError("Only an administrator changes the rate")

    /** RF-RES-03: someone else reserved one of these nights in the meantime. */
    data class NightsTaken(val nights: List<CalendarDate>) : ReservationError("Some nights are no longer free")
    data object NoSession : ReservationError("There is no session")
    data object NotAllowed : ReservationError("Only reception and the administrators make reservations")

    /** RNF-04: reservations need connection, so two devices never hold the same night. */
    data object NoConnection : ReservationError("There is no connection")
    data object Unknown : ReservationError("Unknown error")
}

/** RF-RES-02 and decision 4A: a stay that starts today or later and lasts from 1 to 30 nights. */
fun Stay.check(today: CalendarDate): ReservationError? = when {
    checkOut <= checkIn -> ReservationError.CheckOutNotAfterCheckIn
    nightCount > MAX_RESERVATION_NIGHTS -> ReservationError.TooManyNights
    checkIn < today -> ReservationError.ArrivalInThePast
    else -> null
}

/** RF-RES-02: at least one adult, and no more people than the room takes. Children count. */
fun checkPeople(adults: Int, children: Int, capacity: Int): ReservationError? = when {
    adults < 1 -> ReservationError.NoAdults
    children < 0 || adults + children > capacity -> ReservationError.OverCapacity
    else -> null
}
