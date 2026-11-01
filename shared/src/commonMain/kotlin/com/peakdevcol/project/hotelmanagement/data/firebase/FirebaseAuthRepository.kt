package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    override suspend fun login(email: String, password: String): Result<Unit> = authCall {
        auth.value.signInWithEmailAndPassword(email, password)
    }

    override suspend fun createAccount(user: NewUser, password: String): Result<Unit> = authCall {
        val result = auth.value.createUserWithEmailAndPassword(user.email, password)
        val userId = checkNotNull(result.user?.uid) { "Firebase did not return the new user" }
        // Creating the account starts a session, which swaps the screen and clears the
        // sign-up ViewModel. The profile must be saved even if that cancels the caller.
        withContext(NonCancellable) {
            firestore.value
                .collection(FirestorePaths.USERS)
                .document(FirestorePaths.PROVIDER_BASIC)
                .collection(FirestorePaths.USERS)
                .document(userId)
                .set(user.toDto(userId, FirestorePaths.PROVIDER_BASIC))
        }
    }

    override suspend fun logOut() {
        auth.value.signOut()
    }

    private inline fun authCall(block: () -> Any?): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }

    private fun Exception.toAuthError(): AuthError = when (this) {
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
        is FirebaseNetworkException -> AuthError.Network
        else -> AuthError.Unknown(this)
    }
}
