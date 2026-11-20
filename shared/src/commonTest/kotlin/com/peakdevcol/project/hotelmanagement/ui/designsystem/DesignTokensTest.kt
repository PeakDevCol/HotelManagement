package com.peakdevcol.project.hotelmanagement.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ThousandsSeparatorTransformation
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.DarkMajesticColors
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.LightMajesticColors
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticColors
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesignTokensTest {
    private val themes = mapOf("claro" to LightMajesticColors, "oscuro" to DarkMajesticColors)

    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.luminance().toDouble()
        val b = background.luminance().toDouble()
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun failures(minimum: Double, pairs: (MajesticColors) -> List<Triple<String, Color, Color>>): List<String> =
        themes.flatMap { (theme, colors) ->
            pairs(colors).mapNotNull { (name, foreground, background) ->
                val ratio = contrast(foreground, background)
                if (ratio < minimum) "$theme: $name = ${(ratio * 100).toInt() / 100.0}" else null
            }
        }

    // RNF-18: every text token reads at 4.5:1 on the grounds its note names, in both themes.
    @Test
    fun textIsLegibleOnEverySurface() {
        val problems = failures(4.5) { colors ->
            val texts = mapOf(
                "ink" to colors.ink, "ink-muted" to colors.inkMuted, "ink-faint" to colors.inkFaint,
                "primary" to colors.primary, "success" to colors.success, "warning" to colors.warning, "danger" to colors.danger,
            )
            val grounds = mapOf("surface" to colors.surface, "surface-raised" to colors.surfaceRaised, "surface-sunken" to colors.surfaceSunken)
            texts.flatMap { (text, foreground) -> grounds.map { (ground, background) -> Triple("$text sobre $ground", foreground, background) } }
        }
        assertTrue(problems.isEmpty(), "Textos que no cumplen 4,5:1: $problems")
    }

    @Test
    fun textOnFilledColorsIsLegible() {
        val problems = failures(4.5) { colors ->
            listOf(
                Triple("on-primary sobre primary", colors.onPrimary, colors.primary),
                Triple("on-primary-soft sobre primary-soft", colors.onPrimarySoft, colors.primarySoft),
                Triple("success sobre success-soft", colors.success, colors.successSoft),
                Triple("warning sobre warning-soft", colors.warning, colors.warningSoft),
                Triple("danger sobre danger-soft", colors.danger, colors.dangerSoft),
                Triple("ink sobre warning-soft", colors.ink, colors.warningSoft),
                Triple("ink sobre danger-soft", colors.ink, colors.dangerSoft),
                Triple("on-danger sobre danger", colors.onDanger, colors.danger),
                Triple("on-brand sobre brand", colors.onBrand, colors.brand),
                Triple("on-brand sobre brand-raised", colors.onBrand, colors.brandRaised),
                Triple("on-brand-muted sobre brand", colors.onBrandMuted, colors.brand),
                Triple("on-brand-muted sobre brand-raised", colors.onBrandMuted, colors.brandRaised),
                Triple("on-inverse-surface sobre inverse-surface", colors.onInverseSurface, colors.inverseSurface),
                Triple("inverse-primary sobre inverse-surface", colors.inversePrimary, colors.inverseSurface),
                Triple("surface-raised sobre warning (conteo)", colors.surfaceRaised, colors.warning),
            )
        }
        assertTrue(problems.isEmpty(), "Textos que no cumplen 4,5:1: $problems")
    }

    // Control borders and marks that carry meaning need 3:1 (WCAG 1.4.11).
    @Test
    fun controlBordersAreVisible() {
        val problems = failures(3.0) { colors ->
            listOf(
                Triple("line-strong sobre surface", colors.lineStrong, colors.surface),
                Triple("line-strong sobre surface-raised", colors.lineStrong, colors.surfaceRaised),
                Triple("line-strong sobre surface-sunken", colors.lineStrong, colors.surfaceSunken),
                Triple("primary sobre surface-sunken (barras)", colors.primary, colors.surfaceSunken),
            )
        }
        assertTrue(problems.isEmpty(), "Bordes que no cumplen 3:1: $problems")
    }

    @Test
    fun moneyIsShownWithThousandsDots() {
        val transformed = ThousandsSeparatorTransformation.filter(AnnotatedString("1250000"))

        assertEquals("1.250.000", transformed.text.text)
        assertEquals(9, transformed.offsetMapping.originalToTransformed(7))
        assertEquals(6, transformed.offsetMapping.originalToTransformed(4))
        assertEquals(2, transformed.offsetMapping.originalToTransformed(1))
        assertEquals(4, transformed.offsetMapping.transformedToOriginal(5))
        assertEquals("500", ThousandsSeparatorTransformation.filter(AnnotatedString("500")).text.text)
    }

    @Test
    fun initialsTakeTheFirstTwoWords() {
        assertEquals("AM", initialsOf("Ana María Rojas"))
        assertEquals("L", initialsOf("  laura  "))
        assertEquals("", initialsOf(""))
    }
}
