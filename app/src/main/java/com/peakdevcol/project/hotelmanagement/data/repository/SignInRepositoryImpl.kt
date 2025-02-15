package com.peakdevcol.project.hotelmanagement.data.repository

import android.util.Log
import com.peakdevcol.project.hotelmanagement.data.RemoteDataSource
import com.peakdevcol.project.hotelmanagement.data.response.LoginResult
import com.peakdevcol.project.hotelmanagement.domain.SignInRepository
import com.peakdevcol.project.hotelmanagement.ui.signin.model.BaseUserSignIn
import com.peakdevcol.project.hotelmanagement.ui.signin.model.FullUserSignIn
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.EMAIL
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.FULL_NAME
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PASSWORD
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PASSWORD_CONFIRMATION
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.PROVIDER
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants.TYPE
import javax.inject.Inject

class SignInRepositoryImpl @Inject constructor(
    private val remote: RemoteDataSource
) : SignInRepository {
    override suspend fun createAccount(userSignIn: BaseUserSignIn): Boolean {
        return if (userSignIn is FullUserSignIn) {
            when (remote.createAccount(userSignIn.email, userSignIn.password)) {
                LoginResult.Error -> {
                    Log.e("ERROR", "usuario creado")
                    false
                }

                LoginResult.Success -> {
                    createUserTable(userSignIn = userSignIn)
                }

            }
        } else false
    }

    override suspend fun createUserTable(userSignIn: BaseUserSignIn): Boolean {
        val user = mutableMapOf(
            FULL_NAME to userSignIn.fullName,
            EMAIL to userSignIn.email,
            PROVIDER to userSignIn.provider.name.lowercase()
        )
        if (userSignIn is FullUserSignIn) {
            user[PASSWORD] = userSignIn.password
            user[PASSWORD_CONFIRMATION] = userSignIn.passwordConfirmation
            user[TYPE] = userSignIn.type.name.lowercase()
        }
        return remote.createUserTable(user)
    }


}