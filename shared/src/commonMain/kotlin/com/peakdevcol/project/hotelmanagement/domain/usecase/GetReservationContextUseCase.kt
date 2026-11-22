package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** What a new reservation starts from: the first day it can take, and whether the rate can be changed. */
data class ReservationContext(val today: CalendarDate, val canChangeRate: Boolean)

/** RF-RES-02: only an administrator changes the base rate of the type for one reservation. */
class GetReservationContextUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(): ReservationContext {
        val user = authRepository.currentUserId()?.let { userRepository.observeUser(it).first() }
        return ReservationContext(today = settingsRepository.reservationToday(clock), canChangeRate = user?.type == UserType.ADMIN)
    }
}
