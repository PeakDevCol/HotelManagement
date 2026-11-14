package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import kotlinx.serialization.Serializable

/**
 * Profile stored at `Users/{uid}`. Passwords are handled only by Firebase Auth.
 * Profiles are written by hand in the console (S6) until RF-EMP-04, so a missing field
 * must not crash the app: the defaults make that profile invalid instead.
 */
@Serializable
internal data class UserDto(
    val fullName: String = "",
    val email: String = "",
    val type: String = "",
    val area: String = "",
    val active: Boolean = false,
)

/** Returns null when the role or the area is not one of section 8 of the PRD. */
internal fun UserDto.toUser(id: String): User? {
    val userType = UserType.entries.firstOrNull { it.name.lowercase() == type } ?: return null
    val userArea = UserArea.entries.firstOrNull { it.name.lowercase() == area } ?: return null
    return User(id = id, fullName = fullName, email = email, type = userType, area = userArea, active = active)
}
