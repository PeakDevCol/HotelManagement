package com.peakdevcol.project.hotelmanagement.data.repository

import com.peakdevcol.project.hotelmanagement.ui.signin.model.BaseUserSignIn

interface SignInRepository {
    suspend fun createAccount(userSignIn: BaseUserSignIn): Boolean

    suspend fun createUserTable(userSignIn: BaseUserSignIn): Boolean

}