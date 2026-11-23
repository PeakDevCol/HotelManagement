package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AvailableRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.check
import com.peakdevcol.project.hotelmanagement.domain.model.checkPeople
import com.peakdevcol.project.hotelmanagement.domain.model.nightlyRateIn
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomNightRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * RF-RES-03: the active rooms that take this many people and have every night of the stay free.
 * Only the nights held in the range are read (RNF-12), from the server, since reservations need
 * connection (RNF-04). For a reservation being changed (RF-RES-05), its own nights count as free
 * and each room takes the rate of decision 3A.
 */
class FindAvailableRoomsUseCase(
    private val roomRepository: RoomRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val taxRepository: TaxRepository,
    private val roomNightRepository: RoomNightRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    /** The rooms by number. */
    suspend operator fun invoke(stay: Stay, adults: Int, children: Int, changing: Reservation? = null): Result<List<AvailableRoom>> {
        stay.check(settingsRepository.reservationToday(clock))?.let { return Result.failure(it) }
        val heldRooms = roomNightRepository.nightsBetween(stay.checkIn, stay.checkOut)
            .getOrElse { return Result.failure(it) }
            .filter { it.reservationId != changing?.id }
            .map { it.roomId }
            .toSet()
        val types = roomTypeRepository.observeRoomTypes().first().associateBy { it.id }
        val taxes = taxRepository.observeTaxes().first()
        val rooms = roomRepository.observeRooms().first()
        val typeBefore = changing?.let { reservation -> rooms.firstOrNull { it.id == reservation.roomId }?.roomTypeId }
        val available = rooms
            .filter { room -> room.active && room.id !in heldRooms }
            .mapNotNull { room ->
                val type = types[room.roomTypeId] ?: return@mapNotNull null
                if (checkPeople(adults, children, type.capacity) != null) return@mapNotNull null
                AvailableRoom(room = room, type = type, nightlyRate = nightlyRateIn(type, changing, typeBefore), taxRate = taxes.rateOf(type))
            }
            .sortedBy { it.room.number }
        return Result.success(available)
    }
}
