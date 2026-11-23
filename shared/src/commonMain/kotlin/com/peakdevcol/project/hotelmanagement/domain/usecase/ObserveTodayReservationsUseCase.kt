package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.TodayArrival
import com.peakdevcol.project.hotelmanagement.domain.model.TodayReservations
import com.peakdevcol.project.hotelmanagement.domain.model.status
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

/**
 * RF-RES-07 and RF-RES-09: "Hoy", live: the arrivals of the operational day with how their room
 * is, the departures of the day and the reservations whose day of arrival is over. Only what is
 * open is listened to (RNF-12): the confirmed reservations until today and the guests in the hotel.
 */
class ObserveTodayReservationsUseCase(
    private val reservationRepository: ReservationRepository,
    private val roomRepository: RoomRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    operator fun invoke(): Flow<TodayReservations> = flow {
        val today = settingsRepository.reservationToday(clock)
        emitAll(
            combine(
                reservationRepository.observeConfirmedUntil(today),
                reservationRepository.observeCheckedIn(),
                roomRepository.observeRooms(),
            ) { confirmed, checkedIn, rooms ->
                val roomsById = rooms.associateBy { it.id }
                val byRoom = compareBy<Reservation> { it.roomNumber }
                TodayReservations(
                    today = today,
                    arrivals = confirmed.filter { it.stay.checkIn == today }.sortedWith(byRoom)
                        .map { TodayArrival(it, roomsById[it.roomId]?.status) },
                    departures = checkedIn.filter { it.stay.checkOut <= today }.sortedWith(byRoom),
                    overdue = confirmed.filter { it.stay.checkIn < today }.sortedWith(compareBy({ it.stay.checkIn }, { it.roomNumber })),
                )
            },
        )
    }
}
