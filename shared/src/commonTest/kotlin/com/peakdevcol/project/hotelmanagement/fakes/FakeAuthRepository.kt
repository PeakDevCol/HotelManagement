package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    userId: String? = null,
    var loginResult: Result<Unit> = Result.success(Unit),
    var createAccountResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    private val sessionFlow = MutableStateFlow(userId)
    val loginCalls = mutableListOf<Pair<String, String>>()
    val createdUsers = mutableListOf<Pair<NewUser, String>>()

    override val session: Flow<String?> = sessionFlow
    override fun currentUserId(): String? = sessionFlow.value

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls += email to password
        return loginResult
    }

    override suspend fun createAccount(user: NewUser, password: String): Result<Unit> {
        createdUsers += user to password
        return createAccountResult
    }

    override suspend fun logOut() {
        sessionFlow.value = null
    }
}
