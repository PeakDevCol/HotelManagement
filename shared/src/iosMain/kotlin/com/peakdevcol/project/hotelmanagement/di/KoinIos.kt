package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig

/** Entry point for Swift, which cannot use Kotlin default arguments. */
fun initKoinIos(firebaseEnabled: Boolean) {
    // iOS keeps the local data in the user defaults, so it needs no folder.
    initKoin(AppConfig(firebaseEnabled = firebaseEnabled, localDataDirectory = ""))
}
