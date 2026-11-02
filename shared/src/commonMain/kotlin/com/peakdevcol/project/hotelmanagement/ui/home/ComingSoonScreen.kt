package com.peakdevcol.project.hotelmanagement.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.coming_soon
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import org.jetbrains.compose.resources.stringResource

/** Placeholder for menu sections that are not migrated yet. */
@Composable
fun ComingSoonScreen(section: HomeSection, onBack: () -> Unit) {
    Scaffold(topBar = { ScreenTopBar(title = stringResource(section.title()), onBack = onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
