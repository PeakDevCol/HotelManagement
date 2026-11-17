package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeUserRepository(vararg users: User) : UserRepository {
    private val profiles = MutableStateFlow(users.associateBy { it.id })
    var failure: Throwable? = null

    fun save(user: User) {
        profiles.value += user.id to user
    }

    /** Who created each profile, by employee id. */
    val creators = mutableMapOf<String, String>()
    var writeFailure: Throwable? = null

    override suspend fun createProfile(user: User, createdBy: String): Result<Unit> {
        writeFailure?.let { return Result.failure(it) }
        creators[user.id] = createdBy
        save(user)
        return Result.success(Unit)
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        writeFailure?.let { return Result.failure(it) }
        save(user)
        return Result.success(Unit)
    }

    override fun observeEmployees(): Flow<List<User>> =
        failure?.let { error -> flow { throw error } } ?: profiles.map { it.values.toList() }

    override fun observeUser(userId: String): Flow<User?> =
        failure?.let { error -> flow { throw error } } ?: profiles.map { it[userId] }
}
