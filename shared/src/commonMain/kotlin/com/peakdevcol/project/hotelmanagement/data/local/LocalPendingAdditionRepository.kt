package com.peakdevcol.project.hotelmanagement.data.local

import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.repository.PendingAdditionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Keeps the pending additions in a small file of the device, so they survive closing the app (RF-CUR-13). */
internal class LocalPendingAdditionRepository(
    private val store: Lazy<LocalTextStore>,
) : PendingAdditionRepository {

    private val additions by lazy { MutableStateFlow(store.value.read()?.let(PendingAdditionCodec::decode).orEmpty()) }

    override fun observePendingAdditions(): Flow<List<PendingAddition>> = additions

    override suspend fun save(addition: PendingAddition) = change { current ->
        current.filterNot { it.id == addition.id } + addition
    }

    override suspend fun remove(additionIds: Set<String>) = change { current ->
        current.filterNot { it.id in additionIds }
    }

    private fun change(transform: (List<PendingAddition>) -> List<PendingAddition>) {
        additions.update(transform)
        store.value.write(PendingAdditionCodec.encode(additions.value))
    }
}
