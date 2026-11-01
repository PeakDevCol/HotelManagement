package com.peakdevcol.project.hotelmanagement.ui.introduction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.create_account
import com.peakdevcol.project.hotelmanagement.resources.login
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import org.jetbrains.compose.resources.stringResource

@Composable
fun IntroductionScreen(onLoginClick: () -> Unit, onSignUpClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PrimaryButton(
            text = stringResource(Res.string.login),
            onClick = onLoginClick,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        PrimaryButton(
            text = stringResource(Res.string.create_account),
            onClick = onSignUpClick,
            modifier = Modifier.widthIn(max = 480.dp),
        )
    }
}
