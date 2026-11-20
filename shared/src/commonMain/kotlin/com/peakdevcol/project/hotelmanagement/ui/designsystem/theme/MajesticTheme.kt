package com.peakdevcol.project.hotelmanagement.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalMajesticColors = staticCompositionLocalOf { LightMajesticColors }
private val LocalMajesticTypography = staticCompositionLocalOf<MajesticTypography> {
    error("MajesticTheme is missing above this composable")
}

/**
 * RNF-18: the Majestic design system. It follows the light or dark theme of the device and also
 * styles the Material components, so the whole app reads as one.
 */
@Composable
fun MajesticTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkMajesticColors else LightMajesticColors
    val family = manropeFamily()
    val typography = remember(family) { majesticTypography(family) }
    CompositionLocalProvider(
        LocalMajesticColors provides colors,
        LocalMajesticTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            shapes = MajesticShapes,
        ) {
            // Text and icons outside a Surface take the ink of the theme, never a fixed black.
            CompositionLocalProvider(LocalContentColor provides colors.ink, content = content)
        }
    }
}

object MajesticTheme {
    val colors: MajesticColors
        @Composable @ReadOnlyComposable get() = LocalMajesticColors.current

    val typography: MajesticTypography
        @Composable @ReadOnlyComposable get() = LocalMajesticTypography.current
}
