package com.peakdevcol.project.hotelmanagement

/**
 * Settings that each platform entry point decides at launch.
 *
 * @param firebaseEnabled false when the platform has no Firebase configuration
 * (missing google-services.json / GoogleService-Info.plist). The app still starts.
 */
data class AppConfig(
    val firebaseEnabled: Boolean,
)
