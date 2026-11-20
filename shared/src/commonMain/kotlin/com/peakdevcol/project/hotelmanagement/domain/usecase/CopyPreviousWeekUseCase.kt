package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.movedBy
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.startOfBogotaDay
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository

private const val DAYS_IN_A_WEEK = 7

/**
 * RF-ASI-01: copies the shifts of the week before into the week that starts on [monday]. A shift
 * that is already there, same employee and same times, is not copied twice. Returns how many
 * shifts were added.
 */
class CopyPreviousWeekUseCase(
    private val authRepository: AuthRepository,
    private val shiftRepository: ShiftRepository,
) {
    suspend operator fun invoke(monday: CalendarDate): Result<Int> {
        val administratorId = authRepository.currentUserId() ?: return Result.failure(ScheduleError.NoSession)
        val thisWeek = startOfBogotaDay(monday)
        val previousWeek = startOfBogotaDay(monday.plusDays(-DAYS_IN_A_WEEK.toLong()))
        val nextWeek = startOfBogotaDay(monday.plusDays(DAYS_IN_A_WEEK.toLong()))
        val previous = shiftRepository.shifts(previousWeek, thisWeek).getOrElse { return Result.failure(it) }
        if (previous.isEmpty()) return Result.failure(ScheduleError.NothingToCopy)
        val existing = shiftRepository.shifts(thisWeek, nextWeek).getOrElse { return Result.failure(it) }

        val copies = previous.map { it.movedBy(days = DAYS_IN_A_WEEK, newId = newId()) }
            .filterNot { copy -> existing.any { it.isSameAs(copy) } }
        if (copies.isEmpty()) return Result.success(0)
        return shiftRepository.save(copies, savedBy = administratorId).map { copies.size }
    }

    private fun Shift.isSameAs(other: Shift) =
        employeeId == other.employeeId && startAt == other.startAt && endAt == other.endAt
}
