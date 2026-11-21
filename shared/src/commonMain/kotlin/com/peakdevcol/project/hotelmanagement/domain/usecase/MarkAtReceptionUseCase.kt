package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkOutcome
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.toClose
import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * RF-ASI-11: an employee marks on the authorized computer of reception, without location, with
 * their email and password. The entry or the exit comes from whether they have an entry open, so
 * it is one step. The session of the computer is not touched (V1).
 */
class MarkAtReceptionUseCase(
    private val receptionMarkingRepository: ReceptionMarkingRepository,
    private val deviceRepository: DeviceRepository,
    private val settingsRepository: SettingsRepository,
    private val thisDevice: ThisDevice,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(email: String, password: String): Result<MarkOutcome> {
        if (!isAuthorized()) return Result.failure(AttendanceError.DeviceNotAuthorized)
        val maxOpenEntryHours = settingsRepository.observeSettings().first().maxOpenEntryHours
        return receptionMarkingRepository.asEmployee(email.trim(), password) { session ->
            val now = clock.now()
            val open = session.openEntry().getOrElse { return@asEmployee Result.failure(it) }.toClose(now, maxOpenEntryHours)
            val mark = Mark(at = null, deviceTime = now, source = MarkSource.KIOSK, deviceId = thisDevice.id)
            if (open == null) {
                session.saveEntry(AttendanceRecord(newId(), session.employee.id, mark))
                    .map { MarkOutcome(MarkKind.ENTRY, it, now, employeeName = session.employee.fullName) }
            } else {
                session.saveExit(open.id, mark).map {
                    MarkOutcome(MarkKind.EXIT, it, now, workedTime = now - open.entryTime, employeeName = session.employee.fullName)
                }
            }
        }
    }

    private suspend fun isAuthorized(): Boolean =
        thisDevice.canMarkForOthers && deviceRepository.observeDevice(thisDevice.id).first()?.active == true
}
