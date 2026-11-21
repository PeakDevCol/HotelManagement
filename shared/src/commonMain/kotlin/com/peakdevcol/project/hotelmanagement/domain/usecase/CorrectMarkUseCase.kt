package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_REASON_LENGTH
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkTimeChoice
import com.peakdevcol.project.hotelmanagement.domain.model.adminMark
import com.peakdevcol.project.hotelmanagement.domain.model.kind
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.timesError
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceCorrectionRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import kotlin.time.Clock

/**
 * RF-ASI-07 and RF-ASI-06: the administrator says which time counts for a mark. With one tap, the
 * time of the device or the one of the server; any other time needs a reason (decision 2A). Another
 * time for the exit of an entry without one closes it. The marks keep their original values, and
 * the record that comes back is the one as corrected.
 */
class CorrectMarkUseCase(
    private val authRepository: AuthRepository,
    private val correctionRepository: AttendanceCorrectionRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(record: AttendanceRecord, mark: MarkKind, choice: MarkTimeChoice): Result<AttendanceRecord> {
        val administratorId = authRepository.currentUserId() ?: return Result.failure(CorrectionError.NoSession)
        val original = record.markOf(mark)
        val time = when (choice) {
            MarkTimeChoice.DeviceTime -> original?.deviceTime
            MarkTimeChoice.ServerTime -> original?.at
            is MarkTimeChoice.OtherTime -> choice.time
        } ?: return Result.failure(CorrectionError.NothingToChoose)
        val reason = (choice as? MarkTimeChoice.OtherTime)?.reason?.trim()?.take(MAX_REASON_LENGTH)
        if (choice is MarkTimeChoice.OtherTime && reason.isNullOrEmpty()) return Result.failure(CorrectionError.MissingReason)
        val now = clock.now()
        if (time > now) return Result.failure(CorrectionError.InFuture)

        val correction = Correction(id = newId(), kind = choice.kind, mark = mark, time = time, reason = reason, by = administratorId, at = null)
        val missingExit = if (original == null) adminMark(time) else null
        val corrected = record.copy(clockOut = missingExit ?: record.clockOut, corrections = record.corrections + correction)
        corrected.timesError()?.let { return Result.failure(it) }
        return correctionRepository.addCorrection(record.id, correction, missingExit)
            .map { corrected.copy(corrections = record.corrections + correction.copy(at = now)) }
    }
}
