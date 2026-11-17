package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePendingAdditionRepository : PendingAdditionRepository {
    val additions = MutableStateFlow(emptyList<PendingAddition>())

    override fun observePendingAdditions(): Flow<List<PendingAddition>> = additions

    override suspend fun save(addition: PendingAddition) {
        additions.value = additions.value.filterNot { it.id == addition.id } + addition
    }

    override suspend fun remove(additionIds: Set<String>) {
        additions.value = additions.value.filterNot { it.id in additionIds }
    }
}
