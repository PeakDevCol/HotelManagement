package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.CashClosingDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toCashClosing
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toTimestamp
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.repository.CashClosingRepository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Closings are read from the server only: a closing missing from the cache of the device would
// make the next one start in the wrong place. Single-field queries need no composite index.
internal class FirestoreCashClosingRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = 15.seconds,
) : CashClosingRepository {

    private val closings get() = firestore.value.collection(FirestorePaths.CASH_CLOSINGS)

    override suspend fun recentClosings(limit: Int): Result<List<CashClosing>> =
        read(closings.orderBy(CREATED_AT_FIELD, Direction.DESCENDING).limit(limit))

    override suspend fun closingsCreated(from: Instant, until: Instant): Result<List<CashClosing>> =
        read(
            closings.where {
                all(CREATED_AT_FIELD greaterThanOrEqualTo from.toTimestamp(), CREATED_AT_FIELD lessThan until.toTimestamp())
            },
        )

    private suspend fun read(query: Query): Result<List<CashClosing>> = guarded {
        withTimeoutOrNull(serverWait) { query.get(Source.SERVER) }
            ?.let { snapshot ->
                Result.success(
                    snapshot.documents.mapNotNull { it.data<CashClosingDto>(ServerTimestampBehavior.ESTIMATE).toCashClosing(it.id) },
                )
            }
            ?: Result.failure(CashClosingError.NoConnection)
    }

    // RF-CUR-08 applied to closings: the transaction reads the number first, so if another
    // administrator saved it meanwhile, this one fails instead of covering the same payments.
    override suspend fun save(closing: CashClosing): Result<Unit> = guarded {
        withTimeoutOrNull(serverWait) {
            firestore.value.runTransaction {
                val reference = closings.document(closing.number.toString())
                if (get(reference).exists) {
                    Result.failure(CashClosingError.AlreadyClosed)
                } else {
                    set(reference, closing.toDto())
                    Result.success(Unit)
                }
            }
        } ?: Result.failure(CashClosingError.NoConnection)
    }

    private inline fun <T> guarded(block: () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toCashClosingError())
    }

    private fun Exception.toCashClosingError(): CashClosingError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> CashClosingError.NoConnection
        FirestoreExceptionCode.PERMISSION_DENIED -> CashClosingError.NotAllowed
        else -> CashClosingError.Unknown
    }

    private companion object {
        const val CREATED_AT_FIELD = "createdAt"
    }
}
