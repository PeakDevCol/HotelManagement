package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_RESERVATION_NOTES_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationError
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.Stay
import com.peakdevcol.project.hotelmanagement.domain.model.StayPrice
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.check
import com.peakdevcol.project.hotelmanagement.domain.model.checkPeople
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReservationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomNightRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

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
    private val roomRepository: RoomRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val taxRepository: TaxRepository,
    private val roomNightRepository: RoomNightRepository,
    private val reservationRepository: ReservationRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(request: ReservationRequest): Result<Reservation> {
        val user = currentUser() ?: return Result.failure(ReservationError.NoSession)
        if (user.type == UserType.BASIC) return Result.failure(ReservationError.NotAllowed)
        val guest = request.guest ?: return Result.failure(ReservationError.MissingGuest)
        request.stay.check(settingsRepository.reservationToday(clock))?.let { return Result.failure(it) }
        val (room, type) = activeRoom(request.roomId) ?: return Result.failure(ReservationError.MissingRoom)
        checkPeople(request.adults, request.children, type.capacity)?.let { return Result.failure(it) }
        val rate = request.nightlyRate ?: type.baseRate
        if (rate <= 0) return Result.failure(ReservationError.InvalidRate)
        if (rate != type.baseRate && user.type != UserType.ADMIN) return Result.failure(ReservationError.RateNotAllowed)
        nightsTaken(room, request.stay).getOrElse { return Result.failure(it) }?.let { return Result.failure(it) }

        val price = StayPrice(nightlyRate = rate, nights = request.stay.nightCount, taxRate = taxRepository.observeTaxes().first().rateOf(type))
        val reservation = Reservation(
            id = newId(),
            guestId = guest.id,
            guestName = guest.fullName,
            roomId = room.id,
            roomNumber = room.number,
            stay = request.stay,
            adults = request.adults,
            children = request.children,
            nightlyRates = List(price.nights) { rate },
            taxAmount = price.taxAmount,
            source = request.source,
            notes = request.notes.trim().take(MAX_RESERVATION_NOTES_LENGTH),
            createdBy = user.id,
        )
        return reservationRepository.create(reservation).fold(
            onSuccess = { Result.success(reservation) },
            // The rules reject a night someone else took in the meantime: it reads as that, if so.
            onFailure = { error -> Result.failure(nightsTaken(room, request.stay).getOrNull() ?: error) },
        )
    }

    private suspend fun currentUser(): User? = authRepository.currentUserId()?.let { userRepository.observeUser(it).first() }

    private suspend fun activeRoom(roomId: String?): Pair<Room, RoomType>? {
        val room = roomRepository.observeRooms().first().firstOrNull { it.id == roomId && it.active } ?: return null
        val type = roomTypeRepository.observeRoomTypes().first().firstOrNull { it.id == room.roomTypeId } ?: return null
        return room to type
    }

    /** The nights of the stay that another reservation holds in [room], or null when all are free. */
    private suspend fun nightsTaken(room: Room, stay: Stay): Result<ReservationError.NightsTaken?> =
        roomNightRepository.nightsBetween(stay.checkIn, stay.checkOut).map { nights ->
            nights.filter { it.roomId == room.id }.map { it.date }.sorted().takeIf { it.isNotEmpty() }?.let(ReservationError::NightsTaken)
        }
}
