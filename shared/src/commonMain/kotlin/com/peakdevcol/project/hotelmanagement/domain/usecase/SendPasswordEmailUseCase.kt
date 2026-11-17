package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator

/**
 * RF-AUTH-04: emails the link to choose a new password. An employee asks for it from the login,
 * and an administrator sends it again to an employee who did not get the first one.
 */
class SendPasswordEmailUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!UserInputValidator.isValidEmail(cleanEmail)) return Result.failure(EmployeeError.InvalidEmail)
        return authRepository.sendPasswordEmail(cleanEmail)
    }
}
