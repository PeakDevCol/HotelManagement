package com.peakdevcol.project.hotelmanagement.ui.introduction

import android.content.Context
import android.graphics.drawable.Drawable

sealed class IntroductionViewState {
    data object Loading : IntroductionViewState()
    data class Error(
        val context: Context,
        val background: Drawable,
        val title: String,
        val msg: String,
        val positiveMsg: String,
    ) : IntroductionViewState()

}