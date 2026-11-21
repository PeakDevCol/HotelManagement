package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** RF-ASI-06, RF-ASI-08 and RF-ASI-09: the marks of every employee at `Attendance`. */
interface TeamAttendanceRepository {
    /** RF-ASI-09: the entries without exit of every employee, live. Reception and administrators read them. */
    fun observeOpenEntries(): Flow<List<AttendanceRecord>>

    /**
     * RF-ASI-08: reads once from the server the records whose entry reached it in [from, to). Only
     * administrators read them.
     */
    suspend fun records(from: Instant, to: Instant): Result<List<AttendanceRecord>>
}
