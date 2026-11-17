package com.peakdevcol.project.hotelmanagement.data.backup

import com.peakdevcol.project.hotelmanagement.domain.model.BackupError
import com.peakdevcol.project.hotelmanagement.domain.model.BackupSummary
import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlin.time.Instant

/** RF-EMP-07: the backup is written by the desktop app. On the other platforms it is not offered. */
internal expect fun backupRepository(firestore: Lazy<FirebaseFirestore>): BackupRepository

internal object UnavailableBackupRepository : BackupRepository {
    override val isAvailable = false

    override suspend fun export(since: Instant?): Result<BackupSummary> = Result.failure(BackupError.NotAvailable)
}
