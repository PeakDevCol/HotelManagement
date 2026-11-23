package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.MAX_RESERVATION_NOTES_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEvent
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.isOverdue
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationChangeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** Who closes a reservation: reception or an administrator, with a session (diagram 16.3). */
private suspend fun AuthRepository.receptionist(userRepository: UserRepository): Result<String> {
    val user = currentUserId()?.let { userRepository.observeUser(it).first() } ?: return Result.failure(ReservationError.NoSession)
    return if (user.type == UserType.BASIC) Result.failure(ReservationError.NotAllowed) else Result.success(user.id)
}

/** RF-RES-06: cancels a confirmed reservation with its reason, which stays in the history, and releases its nights. */
class CancelReservationUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val changeRepository: ReservationChangeRepository,
) {
    /** [original] is the reservation as the employee saw it. */
    suspend operator fun invoke(original: Reservation, reason: String): Result<Unit> {
        val userId = authRepository.receptionist(userRepository).getOrElse { return Result.failure(it) }
        val cleanReason = reason.trim().take(MAX_RESERVATION_NOTES_LENGTH)
        if (cleanReason.isEmpty()) return Result.failure(ReservationError.BlankReason)
        if (original.state != ReservationState.CONFIRMED) return Result.failure(ReservationError.Changed)
        val event = ReservationEvent(id = newId(), type = ReservationEventType.CANCELLED, by = userId, reason = cleanReason)
        return changeRepository.close(original, event)
    }
}

/**
 * RF-RES-07: marks a reservation whose day of arrival is over without a check-in as not arrived,
 * which releases its nights. The day is the operational one (P17), so a guest who arrives after
 * midnight is not marked before the day ends.
 */
class MarkNoShowUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val changeRepository: ReservationChangeRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    /** [original] is the reservation as the employee saw it. */
    suspend operator fun invoke(original: Reservation): Result<Unit> {
        val userId = authRepository.receptionist(userRepository).getOrElse { return Result.failure(it) }
        if (original.state != ReservationState.CONFIRMED) return Result.failure(ReservationError.Changed)
        if (!original.isOverdue(settingsRepository.reservationToday(clock))) return Result.failure(ReservationError.NotOverdue)
        val event = ReservationEvent(id = newId(), type = ReservationEventType.NO_SHOW, by = userId)
        return changeRepository.close(original, event)
    }
}
