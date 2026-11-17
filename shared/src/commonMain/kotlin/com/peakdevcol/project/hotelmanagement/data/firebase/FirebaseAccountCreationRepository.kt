package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.AccountCreationRepository
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException

/**
 * V1: creating an account with the client SDK signs in with it. So the accounts are created in a
 * second Firebase instance, which keeps its own session and leaves the administrator's alone.
 */
internal class FirebaseAccountCreationRepository(
    private val secondaryAuth: Lazy<FirebaseAuth>,
) : AccountCreationRepository {

    override suspend fun createAccount(email: String, saveProfile: suspend (accountId: String) -> Result<Unit>): Result<String> =
        try {
            // The employee replaces this password through the email, so nobody ever knows it (RF-EMP-04).
            val account = secondaryAuth.value.createUserWithEmailAndPassword(email, password = newId() + newId()).user
                ?: return Result.failure(EmployeeError.Unknown)
            val saved = saveProfile(account.uid)
            // Without profile the account cannot be used and would keep the email taken.
            if (saved.isFailure) runCatching { account.delete() }
            saved.map { account.uid }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toEmployeeError())
        } finally {
            runCatching { secondaryAuth.value.signOut() }
        }

    private fun Exception.toEmployeeError(): EmployeeError = when {
        this is FirebaseAuthUserCollisionException || reports(EMAIL_EXISTS) -> EmployeeError.EmailAlreadyInUse
        this is FirebaseAuthInvalidCredentialsException || reports(INVALID_EMAIL) -> EmployeeError.InvalidEmail
        this is FirebaseNetworkException -> EmployeeError.Network
        else -> EmployeeError.Unknown
    }

    // The desktop SDK does not use the exception types of the other platforms: it only passes on
    // the error code of the Firebase Auth server in the message.
    private fun Exception.reports(serverCode: String): Boolean = message?.contains(serverCode) == true

    private companion object {
        const val EMAIL_EXISTS = "EMAIL_EXISTS"
        const val INVALID_EMAIL = "INVALID_EMAIL"
    }
}
