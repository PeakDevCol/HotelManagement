package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.ShiftDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toShift
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toTimestamp
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class FirestoreShiftRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = INVENTORY_SERVER_WAIT,
    private val readWait: Duration = 20.seconds,
) : ShiftRepository {

    private val shifts get() = firestore.value.collection(FirestorePaths.SHIFTS)

    // A range on a single field needs no composite index.
    override fun observeShifts(from: Instant, to: Instant): Flow<List<Shift>> =
        between(shifts, from, to).snapshots.map { it.toShifts() }

    // The rules only let an employee list their own shifts, so the query asks for them by employee.
    // Needs the composite index (employeeId, startAt) of firestore.indexes.json.
    override fun observeShiftsOf(employeeId: String, from: Instant, to: Instant): Flow<List<Shift>> =
        between(shifts.where { EMPLOYEE_FIELD equalTo employeeId }, from, to).snapshots.map { it.toShifts() }

    // Source.SERVER: copying a week from a partial cache would leave shifts out without saying so.
    override suspend fun shifts(from: Instant, to: Instant): Result<List<Shift>> = try {
        withTimeoutOrNull(readWait) { between(shifts, from, to).get(Source.SERVER) }
            ?.let { Result.success(it.toShifts()) }
            ?: Result.failure(ScheduleError.Network)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toScheduleError())
    }

    override suspend fun save(shifts: List<Shift>, savedBy: String): Result<Unit> = write {
        val batch = firestore.value.batch()
        shifts.forEach { shift -> batch.set(this.shifts.document(shift.id), shift.toDto(savedBy)) }
        batch.commit()
    }

    override suspend fun delete(shiftId: String): Result<Unit> = write { shifts.document(shiftId).delete() }

    private fun between(query: Query, from: Instant, to: Instant): Query =
        query.where { all(START_FIELD greaterThanOrEqualTo from.toTimestamp(), START_FIELD lessThan to.toTimestamp()) }
            .orderBy(START_FIELD)

    private fun QuerySnapshot.toShifts(): List<Shift> = documents.map { it.data<ShiftDto>().toShift(it.id) }

    /**
     * Firestore applies the change on the device at once and sends it when there is connection
     * (RNF-04). The wait is only there to hear a rejection from the server, like the one a
     * non-administrator gets from the rules.
     */
    private suspend fun write(change: suspend () -> Unit): Result<Unit> = try {
        withTimeoutOrNull(serverWait) { change() }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toScheduleError())
    }

    private fun Exception.toScheduleError(): ScheduleError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.PERMISSION_DENIED -> ScheduleError.NotAllowed
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> ScheduleError.Network
        else -> ScheduleError.Unknown
    }

    private companion object {
        const val EMPLOYEE_FIELD = "employeeId"
        const val START_FIELD = "startAt"
    }
}
