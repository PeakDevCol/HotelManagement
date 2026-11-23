package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.MAX_RESERVATION_NIGHTS
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyCalendar
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyPeriod
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyRoom
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.holdsANightIn
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * RF-RES-04: the rooms by floor and number, live, with the reservations that hold their nights in
 * the period. A reservation has at most 30 nights (decision 4A), so the ones that touch the period
 * arrive from 30 days before it: only those are read (RNF-12), with a range on one field.
 */
class ObserveOccupancyUseCase(
    private val reservationRepository: ReservationRepository,
    private val roomRepository: RoomRepository,
    private val roomTypeRepository: RoomTypeRepository,
) {
    operator fun invoke(period: OccupancyPeriod): Flow<OccupancyCalendar> = combine(
        reservationRepository.observeArrivingBetween(period.firstDay.plusDays(-MAX_RESERVATION_NIGHTS.toLong()), period.lastDay.plusDays(1)),
        roomRepository.observeRooms(),
        roomTypeRepository.observeRoomTypes(),
    ) { reservations, rooms, types ->
        val holding = reservations
            .filter { it.state in HOLDING_STATES && it.holdsANightIn(period) }
            .groupBy { it.roomId }
        val typeNames = types.associate { it.id to it.name }
        // An inactive room still shows while a reservation holds it, so no stay gets lost from sight.
        val shown = rooms
            .filter { room -> room.active || room.id in holding }
            .sortedWith(compareBy({ it.floor }, { it.number }))
            .map { room -> OccupancyRoom(room, typeNames[room.roomTypeId], holding[room.id].orEmpty().sortedBy { it.stay.checkIn }) }
        OccupancyCalendar(period = period, rooms = shown)
    }

    private companion object {
        val HOLDING_STATES = setOf(ReservationState.CONFIRMED, ReservationState.CHECKED_IN)
    }
}
