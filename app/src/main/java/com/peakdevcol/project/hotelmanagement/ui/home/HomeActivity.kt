package com.peakdevcol.project.hotelmanagement.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.core.dialog.BasicDialog
import com.peakdevcol.project.hotelmanagement.databinding.ActivityHomeBinding
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val homeViewModel: HomeViewModel by viewModels()


    companion object {
        fun create(context: Context) = Intent(context, HomeActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initUi()
    }

    private fun initUi() {
        initListeners()
    }


    private fun initListeners() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.perfil -> {
                    true
                }

                R.id.logout -> {
                    showExitDialog()
                    true
                }

                else -> false
            }
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showExitDialog() {
        BasicDialog.create(
            this,
            resources.getDrawable(R.drawable.dialog_bg, this.theme),
            resources.getString(R.string.title_close_session),
            resources.getString(R.string.supporting_text_close_session),
            resources.getString(R.string.accept_close_session)
        ) {
            homeViewModel.logOut()
            it.dismiss()
            startActivity(IntroductionActivity.create(this, auth = false))
            finishAffinity()
        }
    }


}