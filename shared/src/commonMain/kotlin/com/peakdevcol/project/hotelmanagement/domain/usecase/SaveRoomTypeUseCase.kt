package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.MAX_ROOM_CAPACITY
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import kotlinx.coroutines.flow.first

/**
 * RF-HAB-01: creates or edits a type of room, also to deactivate it. Names are not repeated, so a
 * room never shows two types that look the same. Reservations copy the rate of each night (M11),
 * so a new rate only changes the reservations made from then on.
 */
class SaveRoomTypeUseCase(
    private val roomTypeRepository: RoomTypeRepository,
) {
    /** A null [typeId] creates the type. [baseRate] is in pesos, with the tax included (decision 3A). */
    suspend operator fun invoke(
        typeId: String?,
        name: String,
        capacity: Int?,
        baseRate: Long?,
        taxId: String?,
        active: Boolean = true,
    ): Result<Unit> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Result.failure(RoomError.BlankName)
        if (capacity == null || capacity !in 1..MAX_ROOM_CAPACITY) return Result.failure(RoomError.InvalidCapacity)
        if (baseRate == null || baseRate <= 0) return Result.failure(RoomError.InvalidRate)
        val types = roomTypeRepository.observeRoomTypes().first()
        if (types.any { it.id != typeId && it.name.equals(cleanName, ignoreCase = true) }) {
            return Result.failure(RoomError.DuplicateTypeName)
        }
        val type = RoomType(id = typeId ?: newId(), name = cleanName, capacity = capacity, baseRate = baseRate, taxId = taxId, active = active)
        return roomTypeRepository.save(type)
    }
}
