package com.peakdevcol.project.hotelmanagement.domain.model

/** The role decides the permissions (PRD, section 3). The Firestore rules apply the same ones. */
enum class UserType {
    BASIC,
    RECEPTION,
    ADMIN,
}

/** Where the employee works. It decides which tasks they see and the order of their menu. */
enum class UserArea {
    SERVICE,
    RECEPTION,
    HOUSEKEEPING,
    MAINTENANCE,
    MANAGEMENT,
}

/** An employee profile, stored at `Users/{uid}`. Passwords live only in Firebase Auth. */
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val type: UserType,
    val area: UserArea,
    val active: Boolean,
)
