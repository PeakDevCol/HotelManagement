package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalancePeriod
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
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
    private val clock: Clock = Clock.System,
) {
    /** A null [period] is the current month (RF-BAL-01). */
    suspend operator fun invoke(period: BalancePeriod? = null): Result<BalanceReport> {
        val dayStartMinutes = settingsRepository.observeSettings().first().operationalDayStartMinutes
        val chosenPeriod = period ?: BalancePeriod.monthOf(clock.now(), dayStartMinutes)
        if (chosenPeriod.lastDay < chosenPeriod.firstDay) return Result.failure(BalanceError.InvalidPeriod)
        if (chosenPeriod.dayCount > BalancePeriod.MAX_DAYS) return Result.failure(BalanceError.PeriodTooLong)
        return paymentRepository
            .paymentsReceived(chosenPeriod.startsAt(dayStartMinutes), chosenPeriod.endsAt(dayStartMinutes))
            .map { payments -> BalanceReport(chosenPeriod, dayStartMinutes, payments) }
    }
}
