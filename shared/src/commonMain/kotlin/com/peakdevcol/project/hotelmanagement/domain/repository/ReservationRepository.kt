package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEvent
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import kotlinx.coroutines.flow.Flow

/** RF-RES-01: the guests at `Guests`. Their data is personal: only reception and the administrators reach it (S9). */
interface GuestRepository {
    /** Up to [limit] guests that have [key] among their search keys, in no particular order. */
    suspend fun search(key: String, limit: Int): Result<List<Guest>>

    /** The guest with [guestId], or null when it does not exist. */
    suspend fun guest(guestId: String): Result<Guest?>

    suspend fun create(guest: Guest, createdBy: String): Result<Unit>

    /** Changes what the reception types, never the authorization of the data, which the check-in records. */
    suspend fun update(guest: Guest, updatedBy: String): Result<Unit>
}

/** RF-RES-02: the reservations at `Reservations`. They need connection (RNF-04). */
interface ReservationRepository {
    /** The confirmed reservations that arrive on [date] or later, and their changes. */
    fun observeConfirmedFrom(date: CalendarDate): Flow<List<Reservation>>

    /** RF-RES-07 and RF-RES-09: the confirmed reservations that arrive on [date] or before, and their changes. */
    fun observeConfirmedUntil(date: CalendarDate): Flow<List<Reservation>>

    /**
     * RF-RES-04: every reservation, in any state, that arrives from [from] until the day before
     * [until], and their changes.
     */
    fun observeArrivingBetween(from: CalendarDate, until: CalendarDate): Flow<List<Reservation>>

    /** RF-RES-09: the guests in the hotel, whose check-in comes with M12, and their changes. */
    fun observeCheckedIn(): Flow<List<Reservation>>

    /** The reservation with [reservationId] as the server has it, or null when it does not exist. */
    suspend fun reservation(reservationId: String): Result<Reservation?>

    /**
     * RF-RES-03: creates the reservation and holds each of its nights in one transaction, with the
     * time of the server. If another reservation holds a night, nothing is created.
     */
    suspend fun create(reservation: Reservation): Result<Unit>
}

/**
 * RF-RES-05 to RF-RES-07: the changes of a confirmed reservation, each with its [ReservationEvent]
 * in the history and its nights moved or released in the same transaction. They need connection
 * (RNF-04) and fail with `ReservationError.Changed` when the reservation is no longer as [original].
 */
interface ReservationChangeRepository {
    /** Moves the reservation to [updated]: releases the nights it no longer holds and holds the new ones. */
    suspend fun modify(original: Reservation, updated: Reservation, event: ReservationEvent): Result<Unit>

    /** Cancels it or marks it as not arrived, as [event] says, and releases all its nights. */
    suspend fun close(original: Reservation, event: ReservationEvent): Result<Unit>
}

/** Decision 1A of the second PR of M11: the history of a reservation, read when its detail opens. */
interface ReservationHistoryRepository {
    /** Its events, from the oldest. */
    suspend fun events(reservationId: String): Result<List<ReservationEvent>>
}

/** RF-RES-03: the nights the reservations hold, at `RoomNights`. They carry no personal data. */
interface RoomNightRepository {
    /** The nights held from [from] until the night before [until], of every room, read from the server. */
    suspend fun nightsBetween(from: CalendarDate, until: CalendarDate): Result<List<RoomNight>>
}
