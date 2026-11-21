package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceData
import com.peakdevcol.project.hotelmanagement.domain.model.AttendancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.SHIFT_LOOKBACK
import com.peakdevcol.project.hotelmanagement.domain.model.ScheduleError
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TeamAttendanceRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.days

/**
 * RF-ASI-08: reads once from the server the shifts and the marks of a period. Each side has a
 * margin: a night shift that starts the day before, an entry up to two hours before its shift, and
 * a mark that reached the server later than it was made, without connection (RF-ASI-05).
 */
class LoadAttendanceReportUseCase(
    private val shiftRepository: ShiftRepository,
    private val teamAttendanceRepository: TeamAttendanceRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(period: AttendancePeriod): Result<AttendanceData> {
        if (period.dayCount > AttendancePeriod.MAX_DAYS) return Result.failure(ReportError.PeriodTooLong)
        val settings = settingsRepository.observeSettings().first()
        val shifts = shiftRepository.shifts(period.startsAt - SHIFT_LOOKBACK, period.endsAt + SHIFT_LOOKBACK)
            .getOrElse { return Result.failure(it.toReportError()) }
        val records = teamAttendanceRepository.records(period.startsAt - RECORD_MARGIN, period.endsAt + RECORD_MARGIN)
            .getOrElse { return Result.failure(it.toReportError()) }
        return Result.success(
            AttendanceData(
                period = period,
                shifts = shifts,
                records = records,
                lateToleranceMinutes = settings.lateToleranceMinutes,
                maxOpenEntryHours = settings.maxOpenEntryHours,
            ),
        )
    }

    private fun Throwable.toReportError(): ReportError = when (this) {
        is ReportError -> this
        ScheduleError.NotAllowed -> ReportError.NotAllowed
        ScheduleError.Network -> ReportError.Network
        else -> ReportError.Unknown
    }

    private companion object {
        /**
         * The records are asked for by the time of the server, which the employee cannot change. A
         * mark made without connection reaches it later; two days cover a phone that stayed offline
         * a whole shift, without reading much more.
         */
        val RECORD_MARGIN = 2.days
    }
}
