package com.peakdevcol.project.hotelmanagement.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.peakdevcol.project.hotelmanagement.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FirstScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTheme(R.style.Theme_HotelManagement_Starting)
            splashScreen.setKeepOnScreenCondition { true }
        }
    }
}