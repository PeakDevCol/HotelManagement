package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository

/** RF-ASI-01: an administrator takes a shift out of the schedule. A shift is a plan, not a record, so it is deleted. */
class DeleteShiftUseCase(private val shiftRepository: ShiftRepository) {
    suspend operator fun invoke(shiftId: String): Result<Unit> = shiftRepository.delete(shiftId)
}
