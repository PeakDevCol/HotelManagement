package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Emits the employee profile and its changes, or null when the account has no valid profile. */
    fun observeUser(userId: String): Flow<User?>

    /** Emits every employee with a valid profile, active or not: old orders keep the name of who took them. */
    fun observeEmployees(): Flow<List<User>>
}
