package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the id of the logged-in user, or null when there is no session. */
    val session: Flow<String?>

    fun currentUserId(): String?

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun createAccount(user: NewUser, password: String): Result<Unit>

    suspend fun logOut()
}
