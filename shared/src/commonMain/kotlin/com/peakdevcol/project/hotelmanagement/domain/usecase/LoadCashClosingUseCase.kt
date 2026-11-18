package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingOverview
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.repository.CashClosingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PaymentRepository

/**
 * RF-BAL-06: what the next closing covers, from the last closing until now, and the latest
 * closings. Everything is read from the server, so no payment is left out.
 */
class LoadCashClosingUseCase(
    private val cashClosingRepository: CashClosingRepository,
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(): Result<CashClosingOverview> {
        val recentClosings = cashClosingRepository.recentClosings(RECENT_CLOSINGS)
            .getOrElse { return Result.failure(it) }
        val lastClosing = recentClosings.firstOrNull()
        val payments = paymentRepository.paymentsReceivedAfter(lastClosing?.to)
            .getOrElse { return Result.failure(it.toCashClosingError()) }
        return Result.success(CashClosingOverview(CashClosingDraft(lastClosing, payments), recentClosings))
    }

    private fun Throwable.toCashClosingError(): CashClosingError = when (this) {
        PaymentReadError.NoConnection -> CashClosingError.NoConnection
        PaymentReadError.NotAllowed -> CashClosingError.NotAllowed
        else -> CashClosingError.Unknown
    }

    private companion object {
        const val RECENT_CLOSINGS = 10
    }
}
