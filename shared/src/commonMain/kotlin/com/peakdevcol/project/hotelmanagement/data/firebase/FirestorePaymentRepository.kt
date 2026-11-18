package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.PaymentDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toPayment
import com.peakdevcol.project.hotelmanagement.domain.model.Payment
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.repository.PaymentRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class FirestorePaymentRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = 20.seconds,
) : PaymentRepository {

    // Source.SERVER: totals summed from the cache of the device could leave payments out without
    // saying so. A range on a single field needs no composite index (RNF-13).
    override suspend fun paymentsReceived(from: Instant, until: Instant): Result<List<Payment>> = try {
        val query = firestore.value.collection(FirestorePaths.PAYMENTS).where {
            all(
                RECEIVED_AT_FIELD greaterThanOrEqualTo from.toTimestamp(),
                RECEIVED_AT_FIELD lessThan until.toTimestamp(),
            )
        }
        withTimeoutOrNull(serverWait) { query.get(Source.SERVER) }
            ?.let { snapshot ->
                Result.success(snapshot.documents.mapNotNull { it.data<PaymentDto>(ServerTimestampBehavior.ESTIMATE).toPayment(it.id) })
            }
            ?: Result.failure(PaymentReadError.NoConnection)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toPaymentReadError())
    }

    private fun Exception.toPaymentReadError(): PaymentReadError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> PaymentReadError.NoConnection
        FirestoreExceptionCode.PERMISSION_DENIED -> PaymentReadError.NotAllowed
        else -> PaymentReadError.Unknown
    }

    private fun Instant.toTimestamp() = Timestamp(epochSeconds, nanosecondsOfSecond)

    private companion object {
        const val RECEIVED_AT_FIELD = "receivedAt"
    }
}
