package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import kotlinx.coroutines.flow.Flow

interface TaxRepository {
    /** Emits every tax, active or not, and its changes, in no particular order. */
    fun observeTaxes(): Flow<List<Tax>>

    /** Creates the tax, or replaces the one with the same id. Taxes are deactivated, never deleted. */
    suspend fun save(tax: Tax): Result<Unit>
}
