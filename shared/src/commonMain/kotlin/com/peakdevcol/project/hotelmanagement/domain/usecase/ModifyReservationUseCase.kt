package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.MAX_RESERVATION_NOTES_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEvent
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.snapshot
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationChangeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** What changes in a reservation (decision 2A): the stay, the room and the people, and the notes and the source. */
data class ReservationChangeRequest(
    val stayRequest: StayRequest,
    val source: ReservationSource,
    val notes: String,
)

/**
 * RF-RES-05: changes the days, the people or the room of a confirmed reservation with the same
 * checks as a new one; its own nights count as free. The total is recalculated with decision 3A,
 * and the history keeps what it was and what it became (decision 1A). The guest does not change.
 */
class ModifyReservationUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val stayPlanner: StayPlanner,
    private val changeRepository: ReservationChangeRepository,
) {
    /** [original] is the reservation as the employee saw it. */
    suspend operator fun invoke(original: Reservation, request: ReservationChangeRequest): Result<Reservation> {
        val user = authRepository.currentUserId()?.let { userRepository.observeUser(it).first() } ?: return Result.failure(ReservationError.NoSession)
        if (user.type == UserType.BASIC) return Result.failure(ReservationError.NotAllowed)
        if (original.state != ReservationState.CONFIRMED) return Result.failure(ReservationError.Changed)
        val planned = stayPlanner.plan(request.stayRequest, byAdministrator = StayPlanner.canChangeRate(user.type), changing = original)
            .getOrElse { return Result.failure(it) }
        val updated = original.copy(
            roomId = planned.room.id,
            roomNumber = planned.room.number,
            stay = request.stayRequest.stay,
            adults = request.stayRequest.adults,
            children = request.stayRequest.children,
            nightlyRates = List(planned.price.nights) { planned.price.nightlyRate },
            taxAmount = planned.price.taxAmount,
            source = request.source,
            notes = request.notes.trim().take(MAX_RESERVATION_NOTES_LENGTH),
        )
        if (updated == original) return Result.success(original)
        val event = ReservationEvent(id = newId(), type = ReservationEventType.MODIFIED, by = user.id, before = original.snapshot(), after = updated.snapshot())
        return changeRepository.modify(original, updated, event).fold(
            onSuccess = { Result.success(updated) },
            onFailure = { error -> Result.failure(stayPlanner.nightsTaken(updated.roomId, updated.stay, changing = original).getOrNull() ?: error) },
        )
    }
}
