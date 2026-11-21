package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.Mark

/** RF-ASI-07: the changes of the administrator. Each one is a transaction, so it needs connection (RNF-04). */
interface AttendanceCorrectionRepository {
    /**
     * Adds [correction] to the record. With [missingExit] it also writes the exit, only while the
     * record still has none (RF-ASI-06).
     */
    suspend fun addCorrection(recordId: String, correction: Correction, missingExit: Mark?): Result<Unit>

    /** RF-ASI-07 with decision 4A: the record of a shift that has no marks. */
    suspend fun register(record: AttendanceRecord): Result<Unit>
}
