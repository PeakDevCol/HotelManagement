package com.peakdevcol.project.hotelmanagement.data.network

import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.peakdevcol.project.hotelmanagement.data.RemoteDataSource
import com.peakdevcol.project.hotelmanagement.data.response.LoginResult
import com.peakdevcol.project.hotelmanagement.domain.ProviderLogin
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PROVIDER
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.USER_COLLECTION
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.USER_ID
import kotlinx.coroutines.tasks.await

class AuthService(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFireStore: FirebaseFirestore
) : RemoteDataSource {

    /**
     * runCatching its the same that try catch but most efficient because i can get the
     * success or error result in the same place because
     */
    override suspend fun login(
        email: String?,
        password: String?,
        idTokenString: String?
    ): LoginResult =
        runCatching {
            if (idTokenString != null) {
                firebaseAuth.signInWithCredential(
                    GoogleAuthProvider.getCredential(
                        idTokenString,
                        null
                    )
                ).await()
            } else {
                firebaseAuth.signInWithEmailAndPassword(email!!, password!!).await()
            }
        }.toLoginResult()

    override suspend fun logOut(): Boolean {
        if (firebaseAuth.currentUser != null) {
            firebaseAuth.signOut()
            return true
        }
        return false
    }

    override suspend fun createAccount(email: String, password: String): LoginResult = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
    }.toLoginResult()


    override suspend fun createUserTable(user: MutableMap<String, String>): Boolean = runCatching {

        val currentUser = firebaseAuth.currentUser ?: return@runCatching false
        val userId = currentUser.uid
        val providerLogin = user[PROVIDER] ?: return@runCatching false

        val userCollectionRef = firebaseFireStore.collection(USER_COLLECTION)
            .document(providerLogin)
            .collection(USER_COLLECTION)
            .document(userId)

        user[USER_ID] = userId

        if (providerLogin != ProviderLogin.BASIC.name.lowercase()) {
            val providerCollectionRef = firebaseFireStore.collection(USER_COLLECTION)
                .document(providerLogin)
                .collection(USER_COLLECTION)

            val querySnapshot = providerCollectionRef
                .whereEqualTo(USER_ID, user[USER_ID])
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                Log.e("Error", "Email already exists for provider: $providerLogin")
                return@runCatching false
            }
        }

        userCollectionRef.set(user).await()

    }.isSuccess


    /**
     * With this function i can change or map AuthResult to model LoginResult
     */
    private fun Result<AuthResult>.toLoginResult() = when (val result = getOrNull()) {
        null -> LoginResult.Error
        else -> {
            val userId = result.user
            //Make userId Null to see its behavior
            checkNotNull(userId)
            LoginResult.Success
        }
    }
}