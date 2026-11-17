package com.peakdevcol.project.hotelmanagement.desktop

import android.app.Application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.firebase.FirebasePlatform
import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.di.initKoin
import com.peakdevcol.project.hotelmanagement.ui.App
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

fun main() {
    val firebaseEnabled = initFirebase()
    initKoin(AppConfig(firebaseEnabled = firebaseEnabled, localDataDirectory = appDataDirectory().path))

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Hotel Management",
        ) {
            App()
        }
    }
}

/** Starts the JVM Firebase SDK. Returns false when there is no configuration. */
private fun initFirebase(): Boolean {
    val options = generatedFirebaseOptions ?: run {
        System.err.println("Firebase disabled: desktopApp/firebase.properties was not found at build time.")
        return false
    }

    FirebasePlatform.initializeFirebasePlatform(DesktopFirebasePlatform())
    Firebase.initialize(Application(), options)
    return true
}
