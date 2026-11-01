package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import kotlinx.serialization.Serializable

/** User profile stored in Firestore. Passwords are handled only by Firebase Auth. */
@Serializable
internal data class UserDto(
    val userId: String,
    val fullName: String,
    val email: String,
    val provider: String,
    val type: String,
)

internal fun NewUser.toDto(userId: String, provider: String) = UserDto(
    userId = userId,
    fullName = fullName,
    email = email,
    provider = provider,
    type = type.name.lowercase(),
)
