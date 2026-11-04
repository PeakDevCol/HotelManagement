package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.NewUser
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * Profile stored at `Users/{uid}`. Passwords are handled only by Firebase Auth.
 * `area` and `active` arrive in M2 with the Firestore rules, and `createdBy` in M6.
 */
@Serializable
internal data class UserDto(
    val fullName: String,
    val email: String,
    val type: String,
    val createdAt: BaseTimestamp = Timestamp.ServerTimestamp,
)

internal fun NewUser.toDto() = UserDto(
    fullName = fullName,
    email = email,
    type = type.name.lowercase(),
)
