package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.repository.CashClosingRepository
import kotlin.time.Instant

/** Follows the contract of the rules: a closing number can only be saved once. */
class FakeCashClosingRepository(vararg closings: CashClosing) : CashClosingRepository {
    var closings: List<CashClosing> = closings.toList()
    var failure: CashClosingError? = null
    var now: Instant = Instant.parse("2026-11-18T03:00:00Z")

    override suspend fun recentClosings(limit: Int): Result<List<CashClosing>> {
        failure?.let { return Result.failure(it) }
        return Result.success(closings.sortedByDescending { it.number }.take(limit))
    }

    override suspend fun closingsCreated(from: Instant, until: Instant): Result<List<CashClosing>> {
        failure?.let { return Result.failure(it) }
        return Result.success(closings.filter { closing -> closing.createdAt.let { it != null && it >= from && it < until } })
    }

    override suspend fun save(closing: CashClosing): Result<Unit> {
        failure?.let { return Result.failure(it) }
        if (closings.any { it.number == closing.number }) return Result.failure(CashClosingError.AlreadyClosed)
        closings = closings + closing.copy(createdAt = now)
        return Result.success(Unit)
    }
}

fun cashClosing(
    number: Long,
    to: String?,
    createdAt: String,
    from: String? = null,
    type: CashClosingType = CashClosingType.DAILY,
) = CashClosing(
    number = number,
    from = from?.let(Instant::parse),
    to = to?.let(Instant::parse),
    type = type,
    expectedCash = 10_000,
    countedCash = 10_000,
    difference = 0,
    note = "",
    closedBy = "admin",
    createdAt = Instant.parse(createdAt),
)
