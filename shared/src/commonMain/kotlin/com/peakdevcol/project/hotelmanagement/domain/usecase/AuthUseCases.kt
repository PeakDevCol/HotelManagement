package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        authRepository.login(email.trim(), password)
}

class CreateAccountUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(user: NewUser, password: String): Result<Unit> =
        authRepository.createAccount(user.copy(email = user.email.trim()), password)
}

class LogOutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke() = authRepository.logOut()
}

class ObserveSessionUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<String?> = authRepository.session
}
