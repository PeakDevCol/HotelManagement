package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.repository.CashClosingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.PaymentRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * RF-BAL-01 and RF-BAL-02: reads the payments of a period once. V8: the totals and breakdowns
 * are summed on the device, because Firestore aggregations cannot break them down by product.
 */
class LoadBalanceUseCase(
    private val paymentRepository: PaymentRepository,
    private val settingsRepository: SettingsRepository,
    private val cashClosingRepository: CashClosingRepository,
    private val clock: Clock = Clock.System,
) {
    /** A null [period] is the current month (RF-BAL-01). */
    suspend operator fun invoke(period: BalancePeriod? = null): Result<BalanceReport> {
        val dayStartMinutes = settingsRepository.observeSettings().first().operationalDayStartMinutes
        val now = clock.now()
        val chosenPeriod = period ?: BalancePeriod.monthOf(now, dayStartMinutes)
        if (chosenPeriod.lastDay < chosenPeriod.firstDay) return Result.failure(BalanceError.InvalidPeriod)
        if (chosenPeriod.dayCount > BalancePeriod.MAX_DAYS) return Result.failure(BalanceError.PeriodTooLong)
        val from = chosenPeriod.startsAt(dayStartMinutes)
        val until = chosenPeriod.endsAt(dayStartMinutes)
        val payments = paymentRepository.paymentsReceived(from, until).getOrElse { return Result.failure(it) }
        // RF-BAL-06: the closings of the period tell which days ended without a daily closing.
        val closings = cashClosingRepository.closingsCreated(from, until).getOrElse { return Result.failure(it.toPaymentReadError()) }
        return Result.success(
            BalanceReport(
                period = chosenPeriod,
                dayStartMinutes = dayStartMinutes,
                payments = payments,
                closings = closings,
                today = operationalDateOf(now, dayStartMinutes),
            ),
        )
    }

    private fun Throwable.toPaymentReadError(): PaymentReadError = when (this) {
        CashClosingError.NoConnection -> PaymentReadError.NoConnection
        CashClosingError.NotAllowed -> PaymentReadError.NotAllowed
        else -> PaymentReadError.Unknown
    }
}
