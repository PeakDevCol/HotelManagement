package com.peakdevcol.project.hotelmanagement.android

import android.app.Application
import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.di.initKoin

class HotelManagementApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // On Android, Firebase starts automatically from google-services.json.
        initKoin(AppConfig(firebaseEnabled = true))
    }
}
