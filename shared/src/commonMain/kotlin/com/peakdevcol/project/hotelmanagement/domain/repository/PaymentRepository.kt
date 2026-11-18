package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.Payment
import kotlin.time.Instant

interface PaymentRepository {
    /**
     * The payments received from [from], included, to [until], excluded, read from the server.
     * It fails with a [PaymentReadError][com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError].
     */
    suspend fun paymentsReceived(from: Instant, until: Instant): Result<List<Payment>>
}
