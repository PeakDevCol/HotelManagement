package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.repository.TeamAttendanceRepository
import kotlinx.coroutines.flow.Flow

/** RF-ASI-06: every entry without exit, live, so a forgotten one comes up for the administrator as soon as it is. */
class ObserveOpenEntriesUseCase(private val teamAttendanceRepository: TeamAttendanceRepository) {
    operator fun invoke(): Flow<List<AttendanceRecord>> = teamAttendanceRepository.observeOpenEntries()
}
