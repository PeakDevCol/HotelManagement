package com.peakdevcol.project.hotelmanagement.domain.model

/** What the person using the app is allowed to do with their session (RF-AUTH-07). */
sealed interface Access {
    data object NoSession : Access

    /** There is a session, and its employee profile is still being read. */
    data object Checking : Access

    /** The account has no active employee profile, so it cannot use the app. */
    data object Denied : Access

    data class Granted(val user: User) : Access
}
