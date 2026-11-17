package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeCreation
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.domain.repository.AccountCreationRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator

/**
 * RF-EMP-04: an administrator creates an employee with name, email, role and area. The employee
 * gets an email to choose their password, so nobody else knows it.
 */
class CreateEmployeeUseCase(
    private val authRepository: AuthRepository,
    private val accountCreationRepository: AccountCreationRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(fullName: String, email: String, type: UserType, area: UserArea): Result<EmployeeCreation> {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        if (!UserInputValidator.isValidFullName(cleanName)) return Result.failure(EmployeeError.InvalidName)
        if (!UserInputValidator.isValidEmail(cleanEmail)) return Result.failure(EmployeeError.InvalidEmail)
        val administratorId = authRepository.currentUserId() ?: return Result.failure(EmployeeError.NoSession)

        val account = accountCreationRepository.createAccount(cleanEmail) { accountId ->
            val employee = User(id = accountId, fullName = cleanName, email = cleanEmail, type = type, area = area, active = true)
            userRepository.createProfile(employee, createdBy = administratorId)
        }
        account.onFailure { return Result.failure(it) }

        val emailSent = authRepository.sendPasswordEmail(cleanEmail).isSuccess
        return Result.success(if (emailSent) EmployeeCreation.INVITED else EmployeeCreation.CREATED_WITHOUT_EMAIL)
    }
}
