package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.Order
import com.peakdevcol.project.hotelmanagement.domain.repository.OrderRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore

internal class FirestoreOrderRepository(
    private val firestore: Lazy<FirebaseFirestore>,
) : OrderRepository {

    override suspend fun saveOrder(order: Order): Result<Unit> = runCatching {
        firestore.value
            .collection(FirestorePaths.ORDERS)
            .document(order.id)
            .set(order.toDto())
    }
}
