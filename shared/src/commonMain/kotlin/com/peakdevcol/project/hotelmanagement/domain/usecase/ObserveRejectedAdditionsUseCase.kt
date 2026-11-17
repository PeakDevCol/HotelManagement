package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** RF-CUR-13: the products the server did not add, which the employee still has to be told about. */
class ObserveRejectedAdditionsUseCase(
    private val pendingAdditionRepository: PendingAdditionRepository,
) {
    operator fun invoke(): Flow<List<PendingAddition>> =
        pendingAdditionRepository.observePendingAdditions()
            .map { additions -> additions.filter { it.rejected } }
            .distinctUntilChanged()
}
