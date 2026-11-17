package com.peakdevcol.project.hotelmanagement.data.backup

import com.peakdevcol.project.hotelmanagement.domain.repository.BackupRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore

internal actual fun backupRepository(firestore: Lazy<FirebaseFirestore>): BackupRepository = UnavailableBackupRepository
