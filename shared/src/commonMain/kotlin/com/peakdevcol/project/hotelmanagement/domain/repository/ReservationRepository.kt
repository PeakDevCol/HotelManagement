package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import kotlinx.coroutines.flow.Flow

/** RF-RES-01: the guests at `Guests`. Their data is personal: only reception and the administrators reach it (S9). */
interface GuestRepository {
    /** Up to [limit] guests that have [key] among their search keys, in no particular order. */
    suspend fun search(key: String, limit: Int): Result<List<Guest>>

    suspend fun create(guest: Guest, createdBy: String): Result<Unit>

    /** Changes what the reception types, never the authorization of the data, which the check-in records. */
    suspend fun update(guest: Guest, updatedBy: String): Result<Unit>
}

/** RF-RES-02: the reservations at `Reservations`. They need connection (RNF-04). */
interface ReservationRepository {
    /** The confirmed reservations that arrive on [date] or later, and their changes. */
    fun observeConfirmedFrom(date: CalendarDate): Flow<List<Reservation>>

    /**
     * RF-RES-03: creates the reservation and holds each of its nights in one transaction, with the
     * time of the server. If another reservation holds a night, nothing is created.
     */
    suspend fun create(reservation: Reservation): Result<Unit>
}

/** RF-RES-03: the nights the reservations hold, at `RoomNights`. They carry no personal data. */
interface RoomNightRepository {
    /** The nights held from [from] until the night before [until], of every room, read from the server. */
    suspend fun nightsBetween(from: CalendarDate, until: CalendarDate): Result<List<RoomNight>>
}
