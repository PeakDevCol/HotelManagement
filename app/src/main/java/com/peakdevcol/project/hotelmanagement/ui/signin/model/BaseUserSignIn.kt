package com.peakdevcol.project.hotelmanagement.ui.signin.model

import com.peakdevcol.project.hotelmanagement.domain.ProviderLogin
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeUser

sealed class BaseUserSignIn {
    abstract val userId: String?
    abstract val fullName: String
    abstract val email: String
    abstract val provider: ProviderLogin
    abstract val type: ProviderTypeUser
}

data class FullUserSignIn(
    override val userId: String? = null,
    override val fullName: String,
    override val email: String,
    override val provider: ProviderLogin,
    override val type: ProviderTypeUser,
    val password: String = "",
    val passwordConfirmation: String = ""
) : BaseUserSignIn() {
    fun isNotEmpty() =
        fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && passwordConfirmation.isNotEmpty()
}
