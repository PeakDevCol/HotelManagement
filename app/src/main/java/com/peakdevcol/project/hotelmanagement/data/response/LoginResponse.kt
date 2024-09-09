package com.peakdevcol.project.hotelmanagement.data.response

sealed class LoginResult {
    data object Error : LoginResult()
    data object Success: LoginResult()
}