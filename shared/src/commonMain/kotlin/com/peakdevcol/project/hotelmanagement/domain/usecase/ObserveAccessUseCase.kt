package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Access
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * RF-AUTH-07: a Firebase Auth account only gets in while it has an active profile in `Users`.
 * The profile is observed, so deactivating an employee takes their access away at once.
 */
class ObserveAccessUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Access> = authRepository.session.flatMapLatest { userId ->
        if (userId == null) flowOf(Access.NoSession) else observeAccessOf(userId)
    }

    private fun observeAccessOf(userId: String): Flow<Access> =
        userRepository.observeUser(userId)
            .map { user -> if (user != null && user.active) Access.Granted(user) else Access.Denied }
            .onStart { emit(Access.Checking) }
}
