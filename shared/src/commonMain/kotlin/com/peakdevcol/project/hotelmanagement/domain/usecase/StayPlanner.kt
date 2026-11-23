package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.StayPrice
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
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

/** A stay that can be reserved: its room, the type of the room and what it costs. */
data class PlannedStay(val room: Room, val type: RoomType, val price: StayPrice)

/** What a new or changed reservation asks for. A null [nightlyRate] is the rate the room takes (decision 3A). */
data class StayRequest(
    val roomId: String?,
    val stay: Stay,
    val adults: Int,
    val children: Int,
    val nightlyRate: Long?,
)

/**
 * RF-RES-02, RF-RES-03 and RF-RES-05: checks a stay in a room for some people and prices it, the
 * same for a new reservation and for one being changed, whose own nights count as free. Only an
 * administrator gives a rate other than the one the room takes.
 */
class StayPlanner(
    private val roomRepository: RoomRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val taxRepository: TaxRepository,
    private val roomNightRepository: RoomNightRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    suspend fun plan(request: StayRequest, byAdministrator: Boolean, changing: Reservation? = null): Result<PlannedStay> {
        request.stay.check(settingsRepository.reservationToday(clock))?.let { return Result.failure(it) }
        val rooms = roomRepository.observeRooms().first()
        val room = rooms.firstOrNull { it.id == request.roomId && it.active } ?: return Result.failure(ReservationError.MissingRoom)
        val type = roomTypeRepository.observeRoomTypes().first().firstOrNull { it.id == room.roomTypeId }
            ?: return Result.failure(ReservationError.MissingRoom)
        checkPeople(request.adults, request.children, type.capacity)?.let { return Result.failure(it) }
        val typeBefore = changing?.let { reservation -> rooms.firstOrNull { it.id == reservation.roomId }?.roomTypeId }
        val usualRate = nightlyRateIn(type, changing, typeBefore)
        val rate = request.nightlyRate ?: usualRate
        if (rate <= 0) return Result.failure(ReservationError.InvalidRate)
        if (rate != usualRate && !byAdministrator) return Result.failure(ReservationError.RateNotAllowed)
        nightsTaken(room.id, request.stay, changing).getOrElse { return Result.failure(it) }?.let { return Result.failure(it) }
        val taxRate = taxRepository.observeTaxes().first().rateOf(type)
        return Result.success(PlannedStay(room = room, type = type, price = StayPrice(nightlyRate = rate, nights = request.stay.nightCount, taxRate = taxRate)))
    }

    /** The nights of [stay] in [roomId] that another reservation holds, or null when all are free for [changing]. */
    suspend fun nightsTaken(roomId: String, stay: Stay, changing: Reservation? = null): Result<ReservationError.NightsTaken?> =
        roomNightRepository.nightsBetween(stay.checkIn, stay.checkOut).map { nights ->
            nights
                .filter { it.roomId == roomId && it.reservationId != changing?.id }
                .map { it.date }
                .sorted()
                .takeIf { it.isNotEmpty() }
                ?.let(ReservationError::NightsTaken)
        }

    companion object {
        fun canChangeRate(type: UserType): Boolean = type == UserType.ADMIN
    }
}
