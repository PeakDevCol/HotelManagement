package com.peakdevcol.project.hotelmanagement.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.back
import com.peakdevcol.project.hotelmanagement.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Top bar for screens opened from the main menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.back),
                )
            }
        },
    )
}
