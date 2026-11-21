package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toTimestamp
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.repository.TeamAttendanceRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class FirestoreTeamAttendanceRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val readWait: Duration = 20.seconds,
) : TeamAttendanceRepository {

    private val attendance get() = firestore.value.collection(FirestorePaths.ATTENDANCE)

    // One equality needs no composite index. The rules let reception read only these, so the
    // query asks for exactly them.
    override fun observeOpenEntries(): Flow<List<AttendanceRecord>> =
        attendance.where { CLOCK_OUT_AT_FIELD equalTo null }.snapshots.map { it.toRecords() }

    // A range on a single field needs no composite index. Source.SERVER: hours added up from a
    // partial cache would leave marks out without saying so.
    override suspend fun records(from: Instant, to: Instant): Result<List<AttendanceRecord>> = try {
        withTimeoutOrNull(readWait) {
            attendance
                .where { all(CLOCK_IN_AT_FIELD greaterThanOrEqualTo from.toTimestamp(), CLOCK_IN_AT_FIELD lessThan to.toTimestamp()) }
                .get(Source.SERVER)
        }?.let { Result.success(it.toRecords()) } ?: Result.failure(ReportError.Network)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toReportError())
    }

    private fun Exception.toReportError(): ReportError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.PERMISSION_DENIED -> ReportError.NotAllowed
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> ReportError.Network
        else -> ReportError.Unknown
    }
}
