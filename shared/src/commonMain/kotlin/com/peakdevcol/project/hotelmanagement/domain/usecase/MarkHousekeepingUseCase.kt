package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Housekeeping
import com.peakdevcol.project.hotelmanagement.domain.model.Room
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.markableAs
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.HousekeepingRepository

/**
 * RF-HAB-03 with decision 5A: any employee marks a free room clean, or back to be cleaned, in their
 * name. It works without connection, like the cleaning tasks it stands for until phase 4 (RNF-04):
 * the rules only take it if the room is still free and as the employee saw it.
 */
class MarkHousekeepingUseCase(
    private val authRepository: AuthRepository,
    private val housekeepingRepository: HousekeepingRepository,
) {
    /** [room] is the room as the employee saw it. */
    suspend operator fun invoke(room: Room, housekeeping: Housekeeping): Result<Unit> {
        val userId = authRepository.currentUserId() ?: return Result.failure(RoomError.NoSession)
        if (room.markableAs != housekeeping) return Result.failure(RoomError.Changed)
        return housekeepingRepository.mark(room.id, housekeeping, markedBy = userId)
    }
}
