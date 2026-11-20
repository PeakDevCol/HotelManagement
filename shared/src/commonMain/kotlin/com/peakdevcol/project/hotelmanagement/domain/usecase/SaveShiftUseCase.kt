package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftDraft
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository

/**
 * RF-ASI-01: an administrator creates a shift, or changes the one with [shiftId]. A shift that
 * crosses another of the same employee is saved anyway: the schedule warns about it.
 */
class SaveShiftUseCase(
    private val authRepository: AuthRepository,
    private val shiftRepository: ShiftRepository,
) {
    suspend operator fun invoke(shiftId: String?, draft: ShiftDraft): Result<Unit> {
        val shift = draft.toShift(id = shiftId ?: newId()).getOrElse { return Result.failure(it) }
        val administratorId = authRepository.currentUserId() ?: return Result.failure(ScheduleError.NoSession)
        return shiftRepository.save(listOf(shift), savedBy = administratorId)
    }
}
