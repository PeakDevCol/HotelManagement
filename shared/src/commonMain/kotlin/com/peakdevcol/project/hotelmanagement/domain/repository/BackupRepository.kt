package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import kotlin.time.Instant

/** RF-EMP-07: exports the data of the hotel to a file. */
interface BackupRepository {
    /** Only the desktop app can write the file. */
    val isAvailable: Boolean

    /**
     * Writes a JSON file with every collection. With [since], the orders, payments, cash closings,
     * shifts and attendance marks that started before that moment are left out; the rest is
     * always exported whole.
     */
    suspend fun export(since: Instant?): Result<BackupSummary>
}
