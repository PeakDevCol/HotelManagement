package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FirestoreOrderRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = 3.seconds,
) : OrderRepository {

    // RF-ORD-06: Firestore saves the order on the device at once and only then talks to the
    // server. Without connection that answer never comes, so the wait is short: the write
    // stays queued, survives closing the app and is sent when the connection comes back.
    override suspend fun saveOrder(order: Order): Result<OrderDelivery> = try {
        val confirmed = withTimeoutOrNull(serverWait) {
            firestore.value
                .collection(FirestorePaths.ORDERS)
                .document(order.id)
                .set(order.toDto())
        }
        Result.success(if (confirmed == null) OrderDelivery.PENDING else OrderDelivery.SENT)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // An order waits to be sent only while it is new, so the listener covers just the
    // employee's own orders in progress (RNF-12).
    override fun observePendingOrderCount(employeeId: String): Flow<Int> =
        firestore.value
            .collection(FirestorePaths.ORDERS)
            .where { "idEmployee" equalTo employeeId }
            .where { "state" equalTo OrderState.IN_PROGRESS.code() }
            .snapshots(includeMetadataChanges = true)
            .map { orders -> orders.documents.count { it.metadata.hasPendingWrites } }
            .distinctUntilChanged()
}
