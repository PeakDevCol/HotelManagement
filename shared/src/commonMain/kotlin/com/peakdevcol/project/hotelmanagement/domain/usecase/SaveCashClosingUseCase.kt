package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.CashClosingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** RF-BAL-06: an administrator saves the closing of [draft] with the cash counted. It is never edited after. */
class SaveCashClosingUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cashClosingRepository: CashClosingRepository,
) {
    suspend operator fun invoke(draft: CashClosingDraft, type: CashClosingType?, countedCash: Long?, note: String): Result<Unit> {
        draft.validate(type, countedCash, note)?.let { return Result.failure(it) }
        val chosenType = type ?: return Result.failure(CashClosingError.MissingType)
        val counted = countedCash ?: return Result.failure(CashClosingError.InvalidCount)
        val employeeId = authRepository.currentUserId() ?: return Result.failure(CashClosingError.NoSession)
        val employee = userRepository.observeUser(employeeId).first()
        if (employee?.type != UserType.ADMIN) return Result.failure(CashClosingError.NotAllowed)
        return cashClosingRepository.save(draft.toClosing(chosenType, counted, note, closedBy = employeeId))
    }
}
