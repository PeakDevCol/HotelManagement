package com.peakdevcol.project.hotelmanagement.data.repository

import com.peakdevcol.project.hotelmanagement.data.RemoteDataSource
import com.peakdevcol.project.hotelmanagement.data.response.LoginResult
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor(private val remote: RemoteDataSource) :
    LoginRepository {
    override suspend fun login(
        email: String?,
        password: String?,
        idTokenString: String?
    ): LoginResult {
        return remote.login(email, password, idTokenString)
    }

}