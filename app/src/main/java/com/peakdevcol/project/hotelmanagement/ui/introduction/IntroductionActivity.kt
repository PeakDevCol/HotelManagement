package com.peakdevcol.project.hotelmanagement.ui.introduction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.core.dialog.BasicDialog
import com.peakdevcol.project.hotelmanagement.core.dialog.LoadingDialog
import com.peakdevcol.project.hotelmanagement.databinding.ActivityIntroductionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IntroductionActivity : AppCompatActivity() {

    companion object {
        fun create(context: Context, auth: Boolean) =
            Intent(context, IntroductionActivity::class.java).putExtra("auth", auth)
    }

    private val showLoading = LoadingDialog

    private lateinit var binding: ActivityIntroductionBinding

    private val introductionViewModel: IntroductionViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityIntroductionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //if (authUser != null)
        //NAVEGAR PARA HOME
        val authUser = intent.extras?.getBoolean("auth")

        initUi()
    }

    private fun initUi() {
        initObservers()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                introductionViewModel.viewState.collect {
                    updateUi(it)
                }
            }
        }
    }

    private fun updateUi(viewState: IntroductionViewState?) {
        when (viewState) {
            is IntroductionViewState.Error -> {
                showError(viewState)
            }

            IntroductionViewState.Loading -> showLoading.create(this@IntroductionActivity)
            null -> showLoading.dismiss()
        }

    }

    private fun showError(infoError: IntroductionViewState.Error) {
        introductionViewModel.setViewState(IntroductionViewState.Loading)
        BasicDialog.create(
            infoError.context,
            infoError.background,
            infoError.title,
            infoError.msg,
            infoError.positiveMsg,
        ) {
            it.dismiss()
        }
        introductionViewModel.setViewState(null)
    }

}