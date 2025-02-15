package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.data.response.LoginResult

interface LoginRepository {
    suspend fun login(email: String?, password: String?, idTokenString: String?): LoginResult
}