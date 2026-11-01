package com.peakdevcol.project.hotelmanagement.domain.model

enum class UserType {
    ADMIN,
    BASIC
}

/** Data needed to register a new employee. The password is never stored in the database. */
data class NewUser(
    val fullName: String,
    val email: String,
    val type: UserType = UserType.BASIC,
)
