package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.repository.AccountCreationRepository

/** Follows the contract of Firebase: an account whose profile could not be saved is removed again. */
class FakeAccountCreationRepository : AccountCreationRepository {
    /** The accounts that exist, by email. */
    val accounts = mutableMapOf<String, String>()
    var failure: EmployeeError? = null

    override suspend fun createAccount(email: String, saveProfile: suspend (accountId: String) -> Result<Unit>): Result<String> {
        failure?.let { return Result.failure(it) }
        if (email in accounts) return Result.failure(EmployeeError.EmailAlreadyInUse)
        val accountId = "account-${accounts.size + 1}"
        accounts[email] = accountId
        return saveProfile(accountId).map { accountId }.onFailure { accounts.remove(email) }
    }
}
