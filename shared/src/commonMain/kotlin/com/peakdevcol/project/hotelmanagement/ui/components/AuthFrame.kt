package com.peakdevcol.project.hotelmanagement.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.footer_hotel
import com.peakdevcol.project.hotelmanagement.resources.footer_since
import com.peakdevcol.project.hotelmanagement.resources.hotel_logo
import com.peakdevcol.project.hotelmanagement.resources.hotel_name
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Hotel header and footer shared by the introduction, login and sign-up screens. */
@Composable
fun AuthFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.hotel_logo),
                contentDescription = null,
                modifier = Modifier.height(120.dp),
            )
            Text(
                text = stringResource(Res.string.hotel_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(content = content)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(Res.string.footer_hotel), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.footer_since),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}
