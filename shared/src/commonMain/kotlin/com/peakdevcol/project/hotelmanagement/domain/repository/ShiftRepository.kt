package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** RF-ASI-01 and RF-ASI-02: the shifts at `Shifts`. Reads are always bounded by dates (RNF-12). */
interface ShiftRepository {
    /** The shifts of every employee that start in [from, to). Only an administrator reads them. */
    fun observeShifts(from: Instant, to: Instant): Flow<List<Shift>>

    /** The shifts of one employee that start in [from, to), the only ones the employee can read. */
    fun observeShiftsOf(employeeId: String, from: Instant, to: Instant): Flow<List<Shift>>

    /** Reads once from the server the shifts that start in [from, to), to copy them. */
    suspend fun shifts(from: Instant, to: Instant): Result<List<Shift>>

    /** Creates or replaces the shifts, all or none. */
    suspend fun save(shifts: List<Shift>, savedBy: String): Result<Unit>

    suspend fun delete(shiftId: String): Result<Unit>
}
