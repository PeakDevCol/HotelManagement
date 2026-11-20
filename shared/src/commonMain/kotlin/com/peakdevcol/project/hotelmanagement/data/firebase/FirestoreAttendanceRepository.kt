package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FirestoreAttendanceRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = 3.seconds,
) : AttendanceRepository {

    private val attendance get() = firestore.value.collection(FirestorePaths.ATTENDANCE)

    override fun observeOpenEntry(employeeId: String): Flow<AttendanceRecord?> =
        attendance.openEntriesOf(employeeId).snapshots.map { it.toOpenEntry() }

    override suspend fun saveEntry(record: AttendanceRecord): Result<MarkDelivery> =
        deliverMark(serverWait) { attendance.document(record.id).set(record.toDto()) }

    override suspend fun saveExit(recordId: String, exit: Mark): Result<MarkDelivery> =
        deliverMark(serverWait) { attendance.document(recordId).updateFields { exitFields(exit) } }
}
