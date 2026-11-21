package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.OpenEntry
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.model.forgottenAt
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TeamAttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** An entry without exit and the name of whose it is, when their profile can be read. */
data class WorkingEntry(val entry: OpenEntry, val employeeName: String?)

/**
 * RF-ASI-09: the entries without exit of the team, by name. Only reception and administrators see
 * them (PRD, section 3): for anyone else, null. The screen leaves out the forgotten ones (RF-ASI-06).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWhoIsWorkingUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val teamAttendanceRepository: TeamAttendanceRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<List<WorkingEntry>?> {
        val userId = authRepository.currentUserId() ?: return flowOf(null)
        return userRepository.observeUser(userId)
            .map { user -> user != null && user.type in ALLOWED }
            .distinctUntilChanged()
            .flatMapLatest { allowed -> if (allowed) workingEntries() else flowOf(null) }
    }

    private fun workingEntries(): Flow<List<WorkingEntry>> = combine(
        teamAttendanceRepository.observeOpenEntries(),
        userRepository.observeEmployees(),
        settingsRepository.observeSettings(),
    ) { records, employees, settings ->
        val names = employees.associate { it.id to it.fullName }
        records
            .map { record -> WorkingEntry(OpenEntry(record, record.forgottenAt(settings.maxOpenEntryHours)), names[record.employeeId]) }
            .sortedWith(compareBy({ it.employeeName?.lowercase() }, { it.entry.record.entryTime }))
    }

    private companion object {
        val ALLOWED = setOf(UserType.RECEPTION, UserType.ADMIN)
    }
}
