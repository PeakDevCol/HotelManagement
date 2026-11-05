package com.peakdevcol.project.hotelmanagement.android

import android.app.Application
import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.di.initKoin

class HotelManagementApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase starts automatically only when the build had google-services.json.
        initKoin(AppConfig(firebaseEnabled = BuildConfig.FIREBASE_CONFIGURED))
    }
}
