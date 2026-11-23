package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.ReservationDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.ReservationEventDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toEvent
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toNewDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toReservation
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEvent
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationChangeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationHistoryRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Transaction
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * RF-RES-05 to RF-RES-07. Each change reads the reservation inside the transaction, so a change
 * made meanwhile on another device makes this one fail instead of being overwritten, and writes
 * the reservation, its event and its nights together. The rules release a night only when its
 * reservation no longer holds it.
 */
internal class FirestoreReservationChangeRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = RESERVATION_SERVER_WAIT,
) : ReservationChangeRepository {

    private val reservations get() = firestore.value.collection(FirestorePaths.RESERVATIONS)
    private val nights get() = firestore.value.collection(FirestorePaths.ROOM_NIGHTS)

    override suspend fun modify(original: Reservation, updated: Reservation, event: ReservationEvent): Result<Unit> = change(original) {
        update(
            reservations.document(original.id),
            "roomId" to updated.roomId,
            "roomNumber" to updated.roomNumber,
            RESERVATION_CHECK_IN_FIELD to updated.stay.checkIn.isoText,
            RESERVATION_CHECK_OUT_FIELD to updated.stay.checkOut.isoText,
            "adults" to updated.adults,
            "children" to updated.children,
            "nightlyRates" to updated.nightlyRates,
            "taxAmount" to updated.taxAmount,
            "total" to updated.total,
            "source" to updated.source.code(),
            "notes" to updated.notes,
            *signedBy(event),
        )
        setEvent(original.id, event)
        moveNights(original, updated)
    }

    override suspend fun close(original: Reservation, event: ReservationEvent): Result<Unit> = change(original) {
        val state = if (event.type == ReservationEventType.NO_SHOW) ReservationState.NO_SHOW else ReservationState.CANCELLED
        update(reservations.document(original.id), RESERVATION_STATE_FIELD to state.code(), *signedBy(event))
        setEvent(original.id, event)
        original.roomNights.forEach { night -> delete(nights.document(night.id)) }
    }

    private suspend fun change(original: Reservation, writes: Transaction.() -> Unit): Result<Unit> = guarded {
        withTimeoutOrNull(serverWait) {
            firestore.value.runTransaction {
                val stored = get(reservations.document(original.id))
                val current = if (stored.exists) stored.data<ReservationDto>(ServerTimestampBehavior.ESTIMATE).toReservation(original.id) else null
                if (current == null || !current.isAsSeen(original)) {
                    Result.failure(ReservationError.Changed)
                } else {
                    writes()
                    Result.success(Unit)
                }
            }
        } ?: Result.failure(ReservationError.NoConnection)
    }

    private fun signedBy(event: ReservationEvent): Array<Pair<String, Any?>> = arrayOf(
        "updatedBy" to event.by,
        "updatedAt" to FieldValue.serverTimestamp,
        "lastEventId" to event.id,
    )

    private fun Transaction.setEvent(reservationId: String, event: ReservationEvent) {
        set(reservations.document(reservationId).collection(RESERVATION_EVENTS).document(event.id), event.toNewDto())
    }

    // Only what differs: the nights both hold stay, with the mark of the arrival corrected.
    private fun Transaction.moveNights(original: Reservation, updated: Reservation) {
        val before = original.roomNights.associateBy { it.id }
        val after = updated.roomNights.associateBy { it.id }
        (before.keys - after.keys).forEach { id -> delete(nights.document(id)) }
        (after.keys - before.keys).forEach { id -> set(nights.document(id), after.getValue(id).toDto()) }
        (after.keys intersect before.keys)
            .filter { id -> after.getValue(id).arrival != before.getValue(id).arrival }
            .forEach { id -> update(nights.document(id), ROOM_NIGHT_ARRIVAL_FIELD to after.getValue(id).arrival) }
    }

    /** Still confirmed and with everything the employee saw. */
    private fun Reservation.isAsSeen(seen: Reservation): Boolean =
        state == ReservationState.CONFIRMED && copy(createdAt = seen.createdAt) == seen
}

internal class FirestoreReservationHistoryRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = RESERVATION_SERVER_WAIT,
) : ReservationHistoryRepository {

    // Read once, when the detail opens (RNF-12): a reservation has a few changes at most.
    override suspend fun events(reservationId: String): Result<List<ReservationEvent>> = guarded {
        withTimeoutOrNull(serverWait) {
            firestore.value.collection(FirestorePaths.RESERVATIONS).document(reservationId).collection(RESERVATION_EVENTS).get(Source.SERVER)
        }
            ?.let { snapshot ->
                Result.success(
                    snapshot.documents
                        .mapNotNull { it.data<ReservationEventDto>(ServerTimestampBehavior.ESTIMATE).toEvent(it.id) }
                        .sortedBy { it.at },
                )
            }
            ?: Result.failure(ReservationError.NoConnection)
    }
}
