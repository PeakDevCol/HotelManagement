package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.SHIFT_LOOKBACK
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.model.overlappingIds
import com.peakdevcol.project.hotelmanagement.domain.model.startOfBogotaDay
import com.peakdevcol.project.hotelmanagement.domain.model.weekDays
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A shift as the schedule shows it. [overlapsAnother] warns that it crosses another of the same employee. */
data class ScheduledShift(val shift: Shift, val overlapsAnother: Boolean)

/** One day of the schedule, with the shifts that start on it, the earliest first. */
data class ScheduleDay(val date: CalendarDate, val shifts: List<ScheduledShift>)

/** RF-ASI-01: the shifts of every employee in the week that starts on a Monday. */
class ObserveScheduleWeekUseCase(private val shiftRepository: ShiftRepository) {
    operator fun invoke(monday: CalendarDate): Flow<List<ScheduleDay>> {
        val days = weekDays(monday)
        // From the day before, so a night shift that crosses into Monday is seen when looking for overlaps.
        val from = startOfBogotaDay(monday) - SHIFT_LOOKBACK
        val to = startOfBogotaDay(days.last().plusDays(1))
        return shiftRepository.observeShifts(from, to).map { shifts ->
            val overlapping = shifts.overlappingIds()
            days.map { date ->
                ScheduleDay(
                    date = date,
                    shifts = shifts
                        .filter { operationalDateOf(it.startAt, dayStartMinutes = 0) == date }
                        .sortedBy { it.startAt }
                        .map { ScheduledShift(it, overlapsAnother = it.id in overlapping) },
                )
            }
        }
    }
}
