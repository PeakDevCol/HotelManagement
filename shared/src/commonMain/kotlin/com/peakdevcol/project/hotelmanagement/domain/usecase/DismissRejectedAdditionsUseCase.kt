package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository

/** The employee read the warning about products that were not added, so it is not shown again. */
class DismissRejectedAdditionsUseCase(
    private val pendingAdditionRepository: PendingAdditionRepository,
) {
    suspend operator fun invoke(additions: List<PendingAddition>) =
        pendingAdditionRepository.remove(additions.map { it.id }.toSet())
}
