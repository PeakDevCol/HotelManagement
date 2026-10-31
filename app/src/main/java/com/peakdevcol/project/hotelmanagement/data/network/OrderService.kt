package com.peakdevcol.project.hotelmanagement.data.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.peakdevcol.project.hotelmanagement.data.OrderRemoteDataSource
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.ORDER_COLLECTION
import kotlinx.coroutines.tasks.await

class OrderService(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFireStore: FirebaseFirestore
) : OrderRemoteDataSource {

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun saveOrder(orderId: String, order: Map<String, Any?>): Boolean =
        runCatching {
            firebaseFireStore.collection(ORDER_COLLECTION)
                .document(orderId)
                .set(order)
                .await()
        }.onFailure {
            Log.e("OrderService", "Error saving order $orderId", it)
        }.isSuccess
}
