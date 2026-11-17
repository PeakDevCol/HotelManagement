package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import com.peakdevcol.project.hotelmanagement.domain.validation.UserInputValidator
import kotlinx.coroutines.flow.first

/**
 * RF-EMP-02, RF-EMP-03 and RF-EMP-05: changes the name, the role, the area or whether an employee
 * is active. Nobody changes their own role or deactivates themselves, so the hotel never runs out
 * of administrators.
 */
class UpdateEmployeeUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(employee: User): Result<Unit> {
        val changed = employee.copy(fullName = employee.fullName.trim())
        if (!UserInputValidator.isValidFullName(changed.fullName)) return Result.failure(EmployeeError.InvalidName)
        val currentUserId = authRepository.currentUserId() ?: return Result.failure(EmployeeError.NoSession)
        val stored = userRepository.observeEmployees().first().firstOrNull { it.id == changed.id }
            ?: return Result.failure(EmployeeError.NotFound)
        val changesOwnAccess = changed.id == currentUserId && (changed.type != stored.type || changed.active != stored.active)
        if (changesOwnAccess) return Result.failure(EmployeeError.OwnAccess)
        return userRepository.updateProfile(changed.copy(email = stored.email))
    }
}
