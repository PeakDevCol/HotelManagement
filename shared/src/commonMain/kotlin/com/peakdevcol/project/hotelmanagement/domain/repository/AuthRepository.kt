package com.peakdevcol.project.hotelmanagement.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the id of the logged-in user, or null when there is no session. */
    val session: Flow<String?>

    fun currentUserId(): String?

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun logOut()

    /**
     * RF-AUTH-04: asks Firebase to email the link to choose a password. It also succeeds when no
     * account has that email, so nobody can find out which emails are registered.
     */
    suspend fun sendPasswordEmail(email: String): Result<Unit>
}
