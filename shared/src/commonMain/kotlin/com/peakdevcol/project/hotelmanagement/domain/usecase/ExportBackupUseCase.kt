package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.model.parseBogotaDate
import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository

/** RF-EMP-07: an administrator exports a backup, whole or from a date on. */
class ExportBackupUseCase(
    private val backupRepository: BackupRepository,
) {
    val isAvailable: Boolean get() = backupRepository.isAvailable

    /** [sinceDate] is a Colombian date like "2026-11-01", or blank for a whole backup. */
    suspend operator fun invoke(sinceDate: String): Result<BackupSummary> {
        if (!backupRepository.isAvailable) return Result.failure(BackupError.NotAvailable)
        val since = if (sinceDate.isBlank()) null else parseBogotaDate(sinceDate) ?: return Result.failure(BackupError.InvalidDate)
        return backupRepository.export(since)
    }
}
