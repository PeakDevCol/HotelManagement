package com.peakdevcol.project.hotelmanagement

/**
 * Settings that each platform entry point decides at launch.
 *
 * @param firebaseEnabled false when the platform has no Firebase configuration
 * (missing google-services.json, GoogleService-Info.plist or firebase.properties). The app still starts.
 * @param localDataDirectory folder of the device where the app keeps its own small files,
 * one that the operating system does not empty.
 * @param platformContext what Firebase needs to start another instance on this platform: the
 * application context on Android and on the desktop, nothing on iOS.
 */
data class AppConfig(
    val firebaseEnabled: Boolean,
    val localDataDirectory: String,
    val platformContext: Any? = null,
)
