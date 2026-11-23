package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationHistory
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationHistoryEntry
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.repository.GuestRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationHistoryRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** A confirmed reservation about to be changed, with its guest, who does not change (decision 2A). */
data class ReservationToChange(val reservation: Reservation, val guest: Guest)

/** RF-RES-05: reads the reservation as the server has it now, so it is changed from what it is. */
class LoadReservationToChangeUseCase(
    private val reservationRepository: ReservationRepository,
    private val guestRepository: GuestRepository,
) {
    suspend operator fun invoke(reservationId: String): Result<ReservationToChange> {
        val reservation = reservationRepository.reservation(reservationId).getOrElse { return Result.failure(it) }
            ?.takeIf { it.state == ReservationState.CONFIRMED }
            ?: return Result.failure(ReservationError.Changed)
        val guest = guestRepository.guest(reservation.guestId).getOrElse { return Result.failure(ReservationError.NoConnection) }
            ?: return Result.failure(ReservationError.MissingGuest)
        return Result.success(ReservationToChange(reservation, guest))
    }
}

/** Decision 1A: the history of a reservation, from its creation, with who made each change. */
class LoadReservationHistoryUseCase(
    private val historyRepository: ReservationHistoryRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(reservation: Reservation): Result<ReservationHistory> {
        val events = historyRepository.events(reservation.id).getOrElse { return Result.failure(it) }
        val names = userRepository.observeEmployees().first().associate { it.id to it.fullName }
        return Result.success(
            ReservationHistory(
                createdByName = reservation.createdBy?.let(names::get),
                entries = events.map { ReservationHistoryEntry(it, names[it.by]) },
            ),
        )
    }
}
