package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Decision 2A of M11: "Próximas", the confirmed reservations that arrive today or later, live, by arrival and room. */
class ObserveUpcomingReservationsUseCase(
    private val reservationRepository: ReservationRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    operator fun invoke(): Flow<List<Reservation>> = flow {
        val today = settingsRepository.reservationToday(clock)
        emitAll(
            reservationRepository.observeConfirmedFrom(today)
                .map { reservations -> reservations.sortedWith(compareBy({ it.stay.checkIn }, { it.roomNumber })) },
        )
    }
}
