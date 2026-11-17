package com.peakdevcol.project.hotelmanagement.domain.repository

/** RF-EMP-04: creates sign-in accounts for employees without touching the session of who creates them (V1). */
interface AccountCreationRepository {
    /**
     * Creates the account for [email] and runs [saveProfile] with its id. An account is useless
     * without its profile (RF-AUTH-07), so when [saveProfile] fails the account is removed again.
     * Returns the id of the account.
     */
    suspend fun createAccount(email: String, saveProfile: suspend (accountId: String) -> Result<Unit>): Result<String>
}
