package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_REASON_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.adminMark
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.timesError
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceCorrectionRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * RF-ASI-07 with decision 4A: for a shift without marks, the administrator writes the entry and the
 * exit, always with a reason. The record says it came from the administrator, not from a mark.
 */
class RegisterMissingMarksUseCase(
    private val authRepository: AuthRepository,
    private val correctionRepository: AttendanceCorrectionRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(employeeId: String, entry: Instant, exit: Instant, reason: String): Result<AttendanceRecord> {
        val administratorId = authRepository.currentUserId() ?: return Result.failure(CorrectionError.NoSession)
        val trimmedReason = reason.trim().take(MAX_REASON_LENGTH)
        if (trimmedReason.isEmpty()) return Result.failure(CorrectionError.MissingReason)
        val now = clock.now()
        if (exit > now) return Result.failure(CorrectionError.InFuture)

        val registration = Correction(
            id = newId(),
            kind = CorrectionKind.REGISTRATION,
            mark = null,
            time = null,
            reason = trimmedReason,
            by = administratorId,
            at = null,
        )
        val record = AttendanceRecord(newId(), employeeId, clockIn = adminMark(entry), clockOut = adminMark(exit), corrections = listOf(registration))
        record.timesError()?.let { return Result.failure(it) }
        return correctionRepository.register(record).map { record.copy(corrections = listOf(registration.copy(at = now))) }
    }
}
