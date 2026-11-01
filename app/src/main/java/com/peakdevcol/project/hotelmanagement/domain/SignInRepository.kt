package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.ui.signin.model.BaseUserSignIn

interface SignInRepository {
    suspend fun createAccount(userSignIn: BaseUserSignIn): Boolean

    suspend fun createUserTable(userSignIn: BaseUserSignIn): Boolean

}