package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Emits the employee profile and its changes, or null when the account has no valid profile. */
    fun observeUser(userId: String): Flow<User?>

    /** Emits every employee with a valid profile, active or not: old orders keep the name of who took them. */
    fun observeEmployees(): Flow<List<User>>

    /** RF-EMP-04: saves the profile of a new employee, recording who created it. */
    suspend fun createProfile(user: User, createdBy: String): Result<Unit>

    /** Changes the name, the role, the area and whether the employee is active. The email never changes. */
    suspend fun updateProfile(user: User): Result<Unit>
}
