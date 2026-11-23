package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.ReservationDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.RoomNightDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toNewDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toReservation
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.RoomNight
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomNightRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class FirestoreReservationRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = RESERVATION_SERVER_WAIT,
) : ReservationRepository {

    private val reservations get() = firestore.value.collection(FirestorePaths.RESERVATIONS)
    private val nights get() = firestore.value.collection(FirestorePaths.ROOM_NIGHTS)

    // RNF-12: only what is open and still to come, up to a limit far above what 30 rooms can hold
    // in the coming weeks. Needs the index `state` + `checkInDate` (firestore.indexes.json).
    override fun observeConfirmedFrom(date: CalendarDate): Flow<List<Reservation>> =
        reservations
            .where {
                all(
                    RESERVATION_STATE_FIELD equalTo ReservationState.CONFIRMED.code(),
                    RESERVATION_CHECK_IN_FIELD greaterThanOrEqualTo date.isoText,
                )
            }
            .orderBy(RESERVATION_CHECK_IN_FIELD)
            .limit(UPCOMING_LIMIT)
            .snapshots
            .map { it.toReservations() }

    // RF-RES-07 and RF-RES-09: what arrives today and what should have arrived already, a handful
    // of documents. The same index as the upcoming ones.
    override fun observeConfirmedUntil(date: CalendarDate): Flow<List<Reservation>> =
        reservations
            .where {
                all(
                    RESERVATION_STATE_FIELD equalTo ReservationState.CONFIRMED.code(),
                    RESERVATION_CHECK_IN_FIELD lessThanOrEqualTo date.isoText,
                )
            }
            .snapshots
            .map { it.toReservations() }

    // At most one per room: a single field, no composite index.
    override fun observeCheckedIn(): Flow<List<Reservation>> =
        reservations.where { RESERVATION_STATE_FIELD equalTo ReservationState.CHECKED_IN.code() }.snapshots.map { it.toReservations() }

    override suspend fun reservation(reservationId: String): Result<Reservation?> = guarded {
        withTimeoutOrNull(serverWait) { reservations.document(reservationId).get(Source.SERVER) }
            ?.let { snapshot ->
                Result.success(if (snapshot.exists) snapshot.data<ReservationDto>(ServerTimestampBehavior.ESTIMATE).toReservation(snapshot.id) else null)
            }
            ?: Result.failure(ReservationError.NoConnection)
    }

    private fun QuerySnapshot.toReservations(): List<Reservation> =
        documents.mapNotNull { it.data<ReservationDto>(ServerTimestampBehavior.ESTIMATE).toReservation(it.id) }

    // A transaction, and not a batch, because a transaction fails without connection instead of
    // waiting on the device (RNF-04). The rules reject a night that already exists, which undoes all.
    override suspend fun create(reservation: Reservation): Result<Unit> = guarded {
        withTimeoutOrNull(serverWait) {
            firestore.value.runTransaction {
                set(reservations.document(reservation.id), reservation.toNewDto())
                reservation.roomNights.forEach { night -> set(nights.document(night.id), night.toDto()) }
            }
            Result.success(Unit)
        } ?: Result.failure(ReservationError.NoConnection)
    }

    private companion object {
        const val UPCOMING_LIMIT = 200
    }
}

internal class FirestoreRoomNightRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = RESERVATION_SERVER_WAIT,
) : RoomNightRepository {

    private val nights get() = firestore.value.collection(FirestorePaths.ROOM_NIGHTS)

    // A range on a single field needs no composite index. The days are written as `2026-11-22`,
    // so comparing them as text is comparing them as days.
    override suspend fun nightsBetween(from: CalendarDate, until: CalendarDate): Result<List<RoomNight>> = guarded {
        withTimeoutOrNull(serverWait) {
            nights
                .where { all(ROOM_NIGHT_DATE_FIELD greaterThanOrEqualTo from.isoText, ROOM_NIGHT_DATE_FIELD lessThan until.isoText) }
                .get(Source.SERVER)
        }
            ?.let { snapshot -> Result.success(snapshot.documents.mapNotNull { it.data<RoomNightDto>().toRoomNight() }) }
            ?: Result.failure(ReservationError.NoConnection)
    }
}

internal inline fun <T> guarded(block: () -> Result<T>): Result<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toReservationError())
}
