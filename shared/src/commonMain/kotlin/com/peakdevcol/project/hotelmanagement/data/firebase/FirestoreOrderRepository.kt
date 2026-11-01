package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import com.peakdevcol.project.hotelmanagement.domain.usecase.OrderError
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FirestoreOrderRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val saveTimeout: Duration = 15.seconds,
) : OrderRepository {

    // Offline, Firestore keeps the write queued and never returns, so the wait is limited.
    // The write stays queued and is sent when the connection comes back.
    override suspend fun saveOrder(order: Order): Result<Unit> = try {
        withTimeout(saveTimeout) {
            firestore.value
                .collection(FirestorePaths.ORDERS)
                .document(order.id)
                .set(order.toDto())
        }
        Result.success(Unit)
    } catch (e: TimeoutCancellationException) {
        Result.failure(OrderError.Timeout)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
