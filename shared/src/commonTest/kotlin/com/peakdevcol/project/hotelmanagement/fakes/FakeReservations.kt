package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEvent
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.searchKeys
import com.peakdevcol.project.hotelmanagement.domain.repository.GuestRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationChangeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationHistoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomNightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

fun day(text: String): CalendarDate = CalendarDate.parse(text)!!

/** A clock stopped at [text], an instant like `2026-11-22T15:00:00Z`. */
fun clockAt(text: String): Clock = object : Clock {
    override fun now(): Instant = Instant.parse(text)
}

fun guest(id: String, fullName: String, phone: String = "3001234567") = Guest(id = id, fullName = fullName, phone = phone)

fun reservation(
    id: String,
    roomId: String,
    checkIn: String,
    checkOut: String,
    roomNumber: Int = 204,
    guestName: String = "Ana Rojas",
    rate: Long = 250_000,
) = Reservation(
    id = id,
    guestId = "guest-$id",
    guestName = guestName,
    roomId = roomId,
    roomNumber = roomNumber,
    stay = Stay(day(checkIn), day(checkOut)),
    adults = 2,
    children = 0,
    nightlyRates = List(Stay(day(checkIn), day(checkOut)).nightCount) { rate },
    taxAmount = 0,
    source = ReservationSource.PHONE,
)

/** The guests, found like Firestore finds them: by one of their search keys. */
class FakeGuestRepository(vararg guests: Guest) : GuestRepository {
    val guests = MutableStateFlow(guests.toList())
    var failure: Throwable? = null
    val searchedKeys = mutableListOf<String>()

    override suspend fun search(key: String, limit: Int): Result<List<Guest>> {
        failure?.let { return Result.failure(it) }
        searchedKeys += key
        return Result.success(guests.value.filter { key in it.searchKeys }.take(limit))
    }

    override suspend fun guest(guestId: String): Result<Guest?> {
        failure?.let { return Result.failure(it) }
        return Result.success(guests.value.firstOrNull { it.id == guestId })
    }

    override suspend fun create(guest: Guest, createdBy: String): Result<Unit> {
        failure?.let { return Result.failure(it) }
        guests.value += guest
        return Result.success(Unit)
    }

    override suspend fun update(guest: Guest, updatedBy: String): Result<Unit> {
        failure?.let { return Result.failure(it) }
        guests.value = guests.value.map { if (it.id == guest.id) guest else it }
        return Result.success(Unit)
    }
}

/**
 * The reservations, the nights they hold and their history. Like the rules of Firestore, a
 * reservation is not created or moved onto a night another one holds, and then nothing of it is
 * stored; like the transaction, a change fails when the reservation is no longer as it was seen.
 */
class FakeReservationRepository(vararg reservations: Reservation) :
    ReservationRepository, RoomNightRepository, ReservationChangeRepository, ReservationHistoryRepository {
    val reservations = MutableStateFlow(reservations.toList())
    val nights = MutableStateFlow(reservations.flatMap { it.roomNights })
    val events = mutableMapOf<String, List<ReservationEvent>>()
    var readFailure: Throwable? = null
    var createFailure: Throwable? = null
    var changeFailure: Throwable? = null

    /** Another device that reserves [night] right before this one creates its reservation. */
    var nightTakenMeanwhile: RoomNight? = null

    override fun observeConfirmedFrom(date: CalendarDate): Flow<List<Reservation>> =
        readFailure?.let { error -> flow { throw error } }
            ?: reservations.map { all -> all.filter { it.state == ReservationState.CONFIRMED && it.stay.checkIn >= date } }

    override fun observeConfirmedUntil(date: CalendarDate): Flow<List<Reservation>> =
        readFailure?.let { error -> flow { throw error } }
            ?: reservations.map { all -> all.filter { it.state == ReservationState.CONFIRMED && it.stay.checkIn <= date } }

    override fun observeCheckedIn(): Flow<List<Reservation>> = reservations.map { all -> all.filter { it.state == ReservationState.CHECKED_IN } }

    override suspend fun reservation(reservationId: String): Result<Reservation?> {
        readFailure?.let { return Result.failure(it) }
        return Result.success(reservations.value.firstOrNull { it.id == reservationId })
    }

    override suspend fun modify(original: Reservation, updated: Reservation, event: ReservationEvent): Result<Unit> {
        changeFailure?.let { return Result.failure(it) }
        nightTakenMeanwhile?.let { nights.value += it }
        if (!isAsStored(original)) return Result.failure(ReservationError.Changed)
        val heldByOthers = nights.value.filter { it.reservationId != original.id }.map { it.id }.toSet()
        if (updated.roomNights.any { it.id in heldByOthers }) return Result.failure(ReservationError.NotAllowed)
        replace(updated)
        nights.value = nights.value.filter { it.reservationId != original.id } + updated.roomNights
        events[original.id] = events[original.id].orEmpty() + event
        return Result.success(Unit)
    }

    override suspend fun close(original: Reservation, event: ReservationEvent): Result<Unit> {
        changeFailure?.let { return Result.failure(it) }
        if (!isAsStored(original)) return Result.failure(ReservationError.Changed)
        val state = if (event.type == ReservationEventType.NO_SHOW) ReservationState.NO_SHOW else ReservationState.CANCELLED
        replace(original.copy(state = state))
        nights.value = nights.value.filter { it.reservationId != original.id }
        events[original.id] = events[original.id].orEmpty() + event
        return Result.success(Unit)
    }

    override suspend fun events(reservationId: String): Result<List<ReservationEvent>> {
        readFailure?.let { return Result.failure(it) }
        return Result.success(events[reservationId].orEmpty())
    }

    private fun isAsStored(seen: Reservation): Boolean = reservations.value.firstOrNull { it.id == seen.id } == seen

    private fun replace(reservation: Reservation) {
        reservations.value = reservations.value.map { if (it.id == reservation.id) reservation else it }
    }

    override suspend fun create(reservation: Reservation): Result<Unit> {
        createFailure?.let { return Result.failure(it) }
        nightTakenMeanwhile?.let { nights.value += it }
        val held = nights.value.map { it.id }.toSet()
        if (reservation.roomNights.any { it.id in held }) return Result.failure(ReservationError.NotAllowed)
        reservations.value += reservation
        nights.value += reservation.roomNights
        return Result.success(Unit)
    }

    override suspend fun nightsBetween(from: CalendarDate, until: CalendarDate): Result<List<RoomNight>> {
        readFailure?.let { return Result.failure(it) }
        return Result.success(nights.value.filter { it.date >= from && it.date < until })
    }
}
