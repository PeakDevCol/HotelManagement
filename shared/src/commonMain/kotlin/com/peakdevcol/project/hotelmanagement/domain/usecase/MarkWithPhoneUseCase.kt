package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.LocationReading
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkOutcome
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.ZoneCheck
import com.peakdevcol.project.hotelmanagement.domain.model.check
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.toClose
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * RF-ASI-03 to RF-ASI-05 and RF-ASI-10: marks the entry or the exit from the phone, only inside the
 * zone of the hotel. The location is read once, here, and only the distance and the accuracy are
 * kept. Without connection the mark waits on the phone, with the time of the phone.
 */
class MarkWithPhoneUseCase(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(kind: MarkKind): Result<MarkOutcome> {
        val employeeId = authRepository.currentUserId() ?: return Result.failure(AttendanceError.NoSession)
        val settings = settingsRepository.observeSettings().first()
        val zone = settings.zone ?: return Result.failure(AttendanceError.NoZone)
        val reading = locationProvider.currentLocation(zone.minAccuracyMeters.toDouble()).toReading().getOrElse { return Result.failure(it) }
        val inside = when (val check = zone.check(reading)) {
            is ZoneCheck.Inside -> check
            is ZoneCheck.Outside -> return Result.failure(AttendanceError.OutsideZone(check.distanceMeters))
            is ZoneCheck.Imprecise -> return Result.failure(AttendanceError.ImpreciseLocation(check.accuracyMeters))
            ZoneCheck.Simulated -> return Result.failure(AttendanceError.SimulatedLocation)
        }

        val now = clock.now()
        val mark = Mark(at = null, deviceTime = now, source = MarkSource.PHONE, distanceMeters = inside.distanceMeters, accuracyMeters = inside.accuracyMeters)
        val open = attendanceRepository.observeOpenEntry(employeeId).first().toClose(now, settings.maxOpenEntryHours)
        return when (kind) {
            MarkKind.ENTRY -> {
                if (open != null) return Result.failure(AttendanceError.AlreadyOpen)
                attendanceRepository.saveEntry(AttendanceRecord(newId(), employeeId, mark)).map { MarkOutcome(MarkKind.ENTRY, it, now) }
            }
            MarkKind.EXIT -> {
                if (open == null) return Result.failure(AttendanceError.NoOpenEntry)
                attendanceRepository.saveExit(open.id, mark)
                    .map { MarkOutcome(MarkKind.EXIT, it, now, workedTime = now - open.entryTime) }
            }
        }
    }
}

/** The reading, or why there is none. */
fun LocationResult.toReading(): Result<LocationReading> = when (this) {
    is LocationResult.Found -> Result.success(reading)
    LocationResult.PermissionDenied -> Result.failure(AttendanceError.LocationPermissionDenied)
    LocationResult.Disabled -> Result.failure(AttendanceError.LocationDisabled)
    LocationResult.Unavailable -> Result.failure(AttendanceError.LocationUnavailable)
    LocationResult.TimedOut -> Result.failure(AttendanceError.LocationTimedOut)
}
