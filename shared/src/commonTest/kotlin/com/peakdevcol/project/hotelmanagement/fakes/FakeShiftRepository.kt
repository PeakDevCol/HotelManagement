package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.ShiftDraft
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.parseClockTime
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class FakeShiftRepository(vararg shifts: Shift) : ShiftRepository {
    val shifts = MutableStateFlow(shifts.toList())
    var readFailure: Throwable? = null
    var writeFailure: ScheduleError? = null

    /** Who saved each batch, in order. */
    val savedBy = mutableListOf<String>()

    override fun observeShifts(from: Instant, to: Instant): Flow<List<Shift>> =
        readFailure?.let { error -> flow { throw error } } ?: shifts.map { it.startingIn(from, to) }

    override fun observeShiftsOf(employeeId: String, from: Instant, to: Instant): Flow<List<Shift>> =
        readFailure?.let { error -> flow { throw error } }
            ?: shifts.map { all -> all.startingIn(from, to).filter { it.employeeId == employeeId } }

    override suspend fun shifts(from: Instant, to: Instant): Result<List<Shift>> =
        readFailure?.let { Result.failure(it) } ?: Result.success(shifts.value.startingIn(from, to))

    override suspend fun save(shifts: List<Shift>, savedBy: String): Result<Unit> {
        writeFailure?.let { return Result.failure(it) }
        val ids = shifts.map { it.id }.toSet()
        this.shifts.value = this.shifts.value.filterNot { it.id in ids } + shifts
        this.savedBy += savedBy
        return Result.success(Unit)
    }

    override suspend fun delete(shiftId: String): Result<Unit> {
        writeFailure?.let { return Result.failure(it) }
        shifts.value = shifts.value.filterNot { it.id == shiftId }
        return Result.success(Unit)
    }

    private fun List<Shift>.startingIn(from: Instant, to: Instant) = filter { it.startAt >= from && it.startAt < to }.sortedBy { it.startAt }
}

/** A shift on a Colombian day, from [start] to [end] written as "06:00"; an earlier end is the next day. */
fun shift(id: String, employeeId: String, day: String, start: String, end: String, area: UserArea? = null): Shift =
    ShiftDraft(employeeId, CalendarDate.parse(day)!!, parseClockTime(start), parseClockTime(end), area).toShift(id).getOrThrow()
