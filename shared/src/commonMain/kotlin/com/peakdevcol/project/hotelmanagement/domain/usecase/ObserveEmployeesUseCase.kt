package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** RF-EMP-01: every employee, active or not, by name. */
class ObserveEmployeesUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Flow<List<User>> =
        userRepository.observeEmployees().map { employees -> employees.sortedBy { it.fullName.lowercase() } }
}
