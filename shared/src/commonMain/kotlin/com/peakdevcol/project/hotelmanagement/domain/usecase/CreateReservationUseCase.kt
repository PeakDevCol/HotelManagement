package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_RESERVATION_NOTES_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** What the reception chose for a new reservation. A null [nightlyRate] is the base rate of the type. */
data class ReservationRequest(
    val guest: Guest?,
    val roomId: String?,
    val stay: Stay,
    val adults: Int,
    val children: Int,
    val nightlyRate: Long?,
    val source: ReservationSource,
    val notes: String,
)

/**
 * RF-RES-02 and RF-RES-03: reserves a free room for a guest. Each night is held in `RoomNights`
 * in the same transaction, and the rules only create a night that nobody holds, so two people
 * reserving at the same time never cross. The total is the rate of each night, with the tax of
 * the type included.
 */
class CreateReservationUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val stayPlanner: StayPlanner,
    private val reservationRepository: ReservationRepository,
) {
    suspend operator fun invoke(request: ReservationRequest): Result<Reservation> {
        val user = authRepository.currentUserId()?.let { userRepository.observeUser(it).first() } ?: return Result.failure(ReservationError.NoSession)
        if (user.type == UserType.BASIC) return Result.failure(ReservationError.NotAllowed)
        val guest = request.guest ?: return Result.failure(ReservationError.MissingGuest)
        val stayRequest = StayRequest(request.roomId, request.stay, request.adults, request.children, request.nightlyRate)
        val planned = stayPlanner.plan(stayRequest, byAdministrator = StayPlanner.canChangeRate(user.type)).getOrElse { return Result.failure(it) }
        val reservation = newReservation(request, guest, planned, user)
        return reservationRepository.create(reservation).fold(
            onSuccess = { Result.success(reservation) },
            // The rules reject a night someone else took in the meantime: it reads as that, if so.
            onFailure = { error -> Result.failure(stayPlanner.nightsTaken(planned.room.id, request.stay).getOrNull() ?: error) },
        )
    }

    private fun newReservation(request: ReservationRequest, guest: Guest, planned: PlannedStay, user: User) = Reservation(
        id = newId(),
        guestId = guest.id,
        guestName = guest.fullName,
        roomId = planned.room.id,
        roomNumber = planned.room.number,
        stay = request.stay,
        adults = request.adults,
        children = request.children,
        nightlyRates = List(planned.price.nights) { planned.price.nightlyRate },
        taxAmount = planned.price.taxAmount,
        source = request.source,
        notes = request.notes.trim().take(MAX_RESERVATION_NOTES_LENGTH),
        createdBy = user.id,
    )
}
