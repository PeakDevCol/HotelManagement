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
import java.util.prefs.Preferences

fun main() {
    val firebaseEnabled = initFirebase()
    initKoin(AppConfig(firebaseEnabled = firebaseEnabled))

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
        System.err.println("Firebase disabled: androidApp/google-services.json was not found at build time.")
        return false
    }

    // The JVM SDK needs somewhere to persist the session between launches.
    val storage = Preferences.userRoot().node("com/peakdevcol/project/hotelmanagement/firebase")
    FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
        override fun store(key: String, value: String) = storage.put(key, value)
        override fun retrieve(key: String): String? = storage.get(key, null)
        override fun clear(key: String) = storage.remove(key)
        override fun log(msg: String) = println(msg)
    })
    Firebase.initialize(Application(), options)
    return true
}
