package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Emits the employee profile and its changes, or null when the account has no valid profile. */
    fun observeUser(userId: String): Flow<User?>
}
