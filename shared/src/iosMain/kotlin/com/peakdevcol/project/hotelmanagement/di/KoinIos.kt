package com.peakdevcol.project.hotelmanagement.di

import com.peakdevcol.project.hotelmanagement.AppConfig

/** Entry point for Swift, which cannot use Kotlin default arguments. */
fun initKoinIos(firebaseEnabled: Boolean) {
    initKoin(AppConfig(firebaseEnabled = firebaseEnabled))
}
