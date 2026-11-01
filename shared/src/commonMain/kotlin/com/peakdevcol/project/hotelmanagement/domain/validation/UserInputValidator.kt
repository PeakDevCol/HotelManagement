package com.peakdevcol.project.hotelmanagement.domain.validation

/** Rules for the data the user types in the login and sign-up forms. */
object UserInputValidator {
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_FULL_NAME_LENGTH = 6

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    fun isValidFullName(fullName: String): Boolean = fullName.trim().length >= MIN_FULL_NAME_LENGTH
}
