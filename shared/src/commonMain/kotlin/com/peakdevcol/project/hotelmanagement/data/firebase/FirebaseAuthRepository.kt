package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Firebase is received as [Lazy] so nothing touches the SDK until it is used. This lets the
 * apps start even when Firebase is not configured on a platform yet.
 */
internal class FirebaseAuthRepository(
    private val auth: Lazy<FirebaseAuth>,
    private val firestore: Lazy<FirebaseFirestore>,
) : AuthRepository {

    override val session: Flow<String?>
        get() = auth.value.authStateChanged.map { it?.uid }

    override fun currentUserId(): String? = auth.value.currentUser?.uid

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.value.signInWithEmailAndPassword(email, password)
        Unit
    }

    override suspend fun createAccount(user: NewUser, password: String): Result<Unit> =
        runCatching {
            val result = auth.value.createUserWithEmailAndPassword(user.email, password)
            val userId = checkNotNull(result.user?.uid) { "Firebase did not return the new user" }
            firestore.value
                .collection(FirestorePaths.USERS)
                .document(FirestorePaths.PROVIDER_BASIC)
                .collection(FirestorePaths.USERS)
                .document(userId)
                .set(user.toDto(userId, FirestorePaths.PROVIDER_BASIC))
        }

    override suspend fun logOut() {
        auth.value.signOut()
    }
}
