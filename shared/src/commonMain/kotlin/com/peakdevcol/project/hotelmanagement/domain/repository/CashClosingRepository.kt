package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import kotlin.time.Instant

/** Every call needs the server. They fail with a [CashClosingError][com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError]. */
interface CashClosingRepository {
    /** The latest closings, the latest first. */
    suspend fun recentClosings(limit: Int): Result<List<CashClosing>>

    /** The closings saved from [from], included, to [until], excluded. */
    suspend fun closingsCreated(from: Instant, until: Instant): Result<List<CashClosing>>

    /**
     * Saves the closing with the server date as its creation. It fails with
     * [AlreadyClosed][com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError.AlreadyClosed]
     * when a closing with its number exists already.
     */
    suspend fun save(closing: CashClosing): Result<Unit>
}
