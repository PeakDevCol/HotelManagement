package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.response.LoginResult


interface RemoteDataSource {

    suspend fun login(email: String?, password: String?, idTokenString: String?): LoginResult
    suspend fun logOut()

    suspend fun createAccount(email: String, password: String): LoginResult
    suspend fun createUserTable(user: MutableMap<String, String>): Boolean

}

interface LocalDataSource {
//TODO IMPLEMENT THE LOCAL METHODS calls from the API
}