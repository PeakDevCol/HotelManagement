package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.OrderDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.OrderEventDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.orderPaymentId
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toEvent
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toEventDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toOrder
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toPaymentDto
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.model.OrderActionError
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.OrderEvent
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.OrderStateChange
import com.peakdevcol.project.hotelmanagement.domain.model.after
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Transaction
import dev.gitlive.firebase.firestore.code
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
    private val transactionWait: Duration = 15.seconds,
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

    // RNF-12: only what is open is listened to. Paid and cancelled orders leave the query by themselves.
    override fun observeOpenOrders(): Flow<List<Order>> =
        firestore.value
            .collection(FirestorePaths.ORDERS)
            .where { "state" inArray OPEN_STATES }
            .snapshots(includeMetadataChanges = true)
            .map { orders -> orders.documents.mapNotNull { it.toOrder() } }

    override fun observeOrder(orderId: String): Flow<Order?> =
        firestore.value
            .collection(FirestorePaths.ORDERS)
            .document(orderId)
            .snapshots(includeMetadataChanges = true)
            .map { snapshot -> if (snapshot.exists) snapshot.toOrder() else null }

    override fun observeOrderEvents(orderId: String): Flow<List<OrderEvent>> =
        orderDocument(orderId)
            .collection(FirestorePaths.ORDER_EVENTS)
            .orderBy("at", Direction.ASCENDING)
            .snapshots
            .map { events ->
                events.documents.mapNotNull { it.data<OrderEventDto>(ServerTimestampBehavior.ESTIMATE).toEvent() }
            }

    // A transaction reads the order from the server, so two devices cannot both change it from the
    // same state (RF-CUR-08). It cannot run without connection (RF-CUR-12).
    override suspend fun changeState(orderId: String, change: OrderStateChange): Result<Unit> = try {
        withTimeoutOrNull(transactionWait) {
            firestore.value.runTransaction { changeStateIn(this, orderId, change) }
        } ?: Result.failure(OrderActionError.NoConnection)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toOrderActionError())
    }

    private suspend fun changeStateIn(transaction: Transaction, orderId: String, change: OrderStateChange): Result<Unit> {
        val orderReference = orderDocument(orderId)
        val snapshot = transaction.get(orderReference)
        if (!snapshot.exists) return Result.failure(OrderActionError.NotFound)
        val order = snapshot.data<OrderDto>(ServerTimestampBehavior.ESTIMATE).toOrder(isPendingToSend = false)
            ?: return Result.failure(OrderActionError.NotFound)
        val newState = order.state.after(change.action) 
            ?: return Result.failure(OrderActionError.StateChanged(order.state))

        transaction.update(orderReference, *change.orderFields(newState))
        transaction.set(orderReference.collection(FirestorePaths.ORDER_EVENTS).document, change.toEventDto())
        if (change is OrderStateChange.Pay) {
            val paymentReference = firestore.value.collection(FirestorePaths.PAYMENTS).document(orderPaymentId(orderId))
            transaction.set(paymentReference, change.toPaymentDto(orderId, orderAmount = order.totalAmount))
        }
        return Result.success(Unit)
    }

    private fun OrderStateChange.orderFields(newState: OrderState): Array<Pair<String, Any?>> {
        val state = "state" to newState.code()
        return when (this) {
            is OrderStateChange.Close ->
                arrayOf(state, "closedAt" to FieldValue.serverTimestamp, "closedBy" to employeeId)
            is OrderStateChange.Reopen -> arrayOf(state)
            is OrderStateChange.Pay -> arrayOf(
                state,
                "paidAt" to FieldValue.serverTimestamp,
                "paidBy" to employeeId,
                "paymentMethod" to method.code(),
                "tipAmount" to tipAmount,
            )
            is OrderStateChange.Cancel -> arrayOf(
                state,
                "cancelReason" to reason,
                "cancelledAt" to FieldValue.serverTimestamp,
                "cancelledBy" to employeeId,
            )
        }
    }

    private fun Exception.toOrderActionError(): OrderActionError = when ((this as? FirebaseFirestoreException)?.code) {
        FirestoreExceptionCode.UNAVAILABLE, FirestoreExceptionCode.DEADLINE_EXCEEDED -> OrderActionError.NoConnection
        FirestoreExceptionCode.PERMISSION_DENIED -> OrderActionError.NotAllowed
        FirestoreExceptionCode.NOT_FOUND -> OrderActionError.NotFound
        else -> OrderActionError.Unknown
    }

    private fun orderDocument(orderId: String) =
        firestore.value.collection(FirestorePaths.ORDERS).document(orderId)

    // An order saved offline has no server date yet: the estimate of the device keeps it readable.
    private fun DocumentSnapshot.toOrder(): Order? =
        data<OrderDto>(ServerTimestampBehavior.ESTIMATE).toOrder(
            isPendingToSend = metadata.hasPendingWrites,
            isUpToDate = !metadata.isFromCache,
        )

    private companion object {
        val OPEN_STATES = listOf(OrderState.IN_PROGRESS.code(), OrderState.CLOSED.code())
    }
}
