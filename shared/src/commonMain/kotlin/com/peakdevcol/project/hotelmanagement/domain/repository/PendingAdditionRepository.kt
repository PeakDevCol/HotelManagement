package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import kotlinx.coroutines.flow.Flow

/** Additions to orders that this device made and the server has not confirmed. They survive closing the app. */
interface PendingAdditionRepository {
    fun observePendingAdditions(): Flow<List<PendingAddition>>

    /** Adds the addition, or replaces the one with the same id. */
    suspend fun save(addition: PendingAddition)

    suspend fun remove(additionIds: Set<String>)
}
