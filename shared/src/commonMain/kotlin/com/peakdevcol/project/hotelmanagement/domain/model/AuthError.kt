package com.peakdevcol.project.hotelmanagement.domain.model

/** Authentication failures the UI knows how to explain to the user. */
sealed class AuthError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object InvalidCredentials : AuthError("Invalid email or password")
    data object EmailAlreadyInUse : AuthError("The email is already registered")
    data object WeakPassword : AuthError("The password is too weak")
    data object Network : AuthError("No network connection")
    class Unknown(cause: Throwable) : AuthError(cause.message ?: "Unknown error", cause)
}
