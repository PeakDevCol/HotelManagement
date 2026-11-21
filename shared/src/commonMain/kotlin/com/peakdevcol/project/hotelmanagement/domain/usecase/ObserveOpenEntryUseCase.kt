package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OpenEntry
import com.peakdevcol.project.hotelmanagement.domain.model.forgottenAt
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/** RF-ASI-03 and RF-ASI-04: the entry without exit of the logged-in employee, which decides what they mark next. */
class ObserveOpenEntryUseCase(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<OpenEntry?> {
        val employeeId = authRepository.currentUserId() ?: return flowOf(null)
        return combine(attendanceRepository.observeOpenEntry(employeeId), settingsRepository.observeSettings()) { record, settings ->
            record?.let { OpenEntry(it, it.forgottenAt(settings.maxOpenEntryHours)) }
        }
    }
}
