package com.peakdevcol.project.hotelmanagement.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FirstScreenActivity : AppCompatActivity() {

    private val firstScreenViewModel: FirstScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTheme(R.style.Theme_HotelManagement_Starting)
            splashScreen.setKeepOnScreenCondition { true }
        }
        initObservers()
        firstScreenViewModel.checkAuthUser()
    }

    private fun initObservers() {
        firstScreenViewModel.authUser.observe(this) {
            it.getContentIfNotHandled()?.let { auth ->
                goToNextScreen(auth)
            }
        }
    }

    private fun goToNextScreen(auth: Boolean) {
        startActivity(IntroductionActivity.create(this,auth))
        finish()
    }

}