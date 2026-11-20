package com.peakdevcol.project.hotelmanagement.ui.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.manrope_bold
import com.peakdevcol.project.hotelmanagement.resources.manrope_extrabold
import com.peakdevcol.project.hotelmanagement.resources.manrope_medium
import com.peakdevcol.project.hotelmanagement.resources.manrope_semibold
import org.jetbrains.compose.resources.Font

/** The type scale of the design system: one family, Manrope, on every platform. */
@Immutable
data class MajesticTypography(
    /** The main figure of a screen, like the total received. One per screen at most. */
    val display: TextStyle,
    /** Screen title on desktop. */
    val headline: TextStyle,
    /** Screen title on a phone, and the title of sheets and dialogs. */
    val titleLarge: TextStyle,
    /** The destination of an order and section titles. */
    val title: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    /** Buttons, chips, tabs and navigation. */
    val label: TextStyle,
    /** Dates, field help and status badges. */
    val caption: TextStyle,
    /** Upper-case label above a figure or a group. Never for sentences. */
    val overline: TextStyle,
    /** Totals of orders and breakdown rows. */
    val amount: TextStyle,
)

// Money lines up in columns only with tabular figures.
private const val TABULAR_FIGURES = "tnum"

@Composable
internal fun manropeFamily(): FontFamily = FontFamily(
    Font(Res.font.manrope_medium, FontWeight.Medium),
    Font(Res.font.manrope_semibold, FontWeight.SemiBold),
    Font(Res.font.manrope_bold, FontWeight.Bold),
    Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
)

internal fun majesticTypography(family: FontFamily) = MajesticTypography(
    display = TextStyle(fontFamily = family, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em, fontFeatureSettings = TABULAR_FIGURES),
    headline = TextStyle(fontFamily = family, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = family, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    title = TextStyle(fontFamily = family, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    body = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    label = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.01.em),
    caption = TextStyle(fontFamily = family, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.01.em),
    overline = TextStyle(fontFamily = family, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
    amount = TextStyle(fontFamily = family, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = TABULAR_FIGURES),
)

/** The same scale for the Material components the app still uses. */
internal fun MajesticTypography.toMaterialTypography() = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = headline,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = titleLarge,
    titleLarge = titleLarge,
    titleMedium = title,
    titleSmall = label,
    bodyLarge = bodyLarge,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = caption,
    labelSmall = caption,
)
