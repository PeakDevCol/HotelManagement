package com.peakdevcol.project.hotelmanagement.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.fail

/** The width, text size and theme a component is drawn with in a layout check. */
internal data class LayoutFrame(val width: Dp, val fontScale: Float, val darkTheme: Boolean = false) {
    override fun toString() = "${width.value.toInt()} dp, letra al ${(fontScale * 100).toInt()} %" + if (darkTheme) ", tema oscuro" else ""
}

internal const val FRAME_TAG = "design-system-frame"

/** A phone or window of [frame] width, with the same 16 dp side margin as the screens. */
@Composable
internal fun LayoutFrameBox(frame: LayoutFrame, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, frame.fontScale)) {
        MajesticTheme(darkTheme = frame.darkTheme) {
            Box(
                Modifier
                    .testTag(FRAME_TAG)
                    .width(frame.width)
                    .background(MajesticTheme.colors.surface)
                    .padding(16.dp),
            ) { content() }
        }
    }
}

// Rounding of fractional pixels.
private const val TOLERANCE_PX = 1f

/**
 * RNF-18, "Contenido que no cabe": nothing leaves the box that holds it, no text is cut without an
 * ellipsis, nothing collapses to nothing, and everything that is touched is at least 48 dp. With
 * [checkSplitWords], no word is broken in two lines either; it is off by default until the screens
 * listed in S11 of the PRD stop breaking them.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.assertNothingSpills(context: String, checkSplitWords: Boolean = false) {
    val problems = mutableListOf<String>()
    val minimumTouch = with(density) { MajesticSizes.control.toPx() } - TOLERANCE_PX
    val frame = onNodeWithTag(FRAME_TAG, useUnmergedTree = true).fetchSemanticsNode()

    fun visit(node: SemanticsNode, container: Rect?, insideScroll: Boolean) {
        // `boundsInRoot` is cut to what the containers show (a Surface clips its content), so the
        // full size is used to see what does not fit, and the visible size to see what got cut.
        val bounds = Rect(node.positionInRoot, node.size.toSize())
        val visible = node.boundsInRoot
        val name = node.describe()
        // Inside a scrolling container, an item scrolled out of view has no visible size on purpose,
        // and a one-line field scrolls the text that is longer than the field.
        // A node that has no size of its own did not scroll away: it collapsed, and is checked.
        val outOfView = insideScroll && (visible.width < TOLERANCE_PX || visible.height < TOLERANCE_PX) &&
            bounds.width >= TOLERANCE_PX && bounds.height >= TOLERANCE_PX
        val editable = node.config.contains(SemanticsProperties.EditableText)
        if (!outOfView && !editable) {
            if (container != null && bounds.width > 0f && bounds.height > 0f && !container.contains(bounds)) {
                problems += "$name se sale de su contenedor (${bounds.short()} fuera de ${container.short()})"
            }
            if (!insideScroll && (visible.width < bounds.width - TOLERANCE_PX || visible.height < bounds.height - TOLERANCE_PX)) {
                problems += "$name queda cortado: se ve ${visible.short()} de ${bounds.short()}"
            }
            node.textLayout()?.let { layout -> problems += layout.problems(name, bounds, checkSplitWords) }
            if (node.config.contains(SemanticsActions.OnClick) && node.config.getOrNull(SemanticsProperties.Disabled) == null) {
                val touch = node.touchBoundsInRoot
                if (touch.width < minimumTouch || touch.height < minimumTouch) problems += "$name: área táctil de ${touch.short()}, menor de 48 dp"
            }
        }
        // What is inside a text field (its background, the text that scrolls) is Compose's own.
        if (editable) return
        // Children of a scrolling container may be outside its visible box on purpose.
        val scrolls = node.config.contains(SemanticsProperties.HorizontalScrollAxisRange) || node.config.contains(SemanticsProperties.VerticalScrollAxisRange)
        node.children.forEach { child -> visit(child, if (scrolls) null else bounds, insideScroll || scrolls) }
    }

    visit(frame, null, insideScroll = false)
    if (problems.isNotEmpty()) fail("$context:\n" + problems.distinct().joinToString("\n") { "  - $it" })
}

/**
 * What is wrong with how a text was laid out. The paragraph is measured at the full width it was
 * offered, so its own overflow flags are not reliable: each line is measured instead. A text set
 * to end with an ellipsis may be shortened; any other text must show whole.
 */
private fun TextLayoutResult.problems(name: String, bounds: Rect, checkSplitWords: Boolean): List<String> {
    val problems = mutableListOf<String>()
    if (bounds.width < TOLERANCE_PX || bounds.height < TOLERANCE_PX) problems += "$name: el texto quedó colapsado"
    val shortensWithEllipsis = layoutInput.overflow == TextOverflow.Ellipsis
    // Shortened is fine; shortened down to the "…" alone, less than one letter wide, is not. A
    // word as short as "TI" is that narrow whole, so only a text wider than its space counts.
    val oneLetter = with(layoutInput.density) { layoutInput.style.fontSize.toPx() }
    val shortened = multiParagraph.intrinsics.maxIntrinsicWidth > size.width + TOLERANCE_PX
    if (shortensWithEllipsis && shortened && layoutInput.text.length > 1 && size.width < oneLetter) {
        problems += "$name: el texto se acorta hasta no dejar nada (${size.width} px)"
    }
    val widestLine = (0 until lineCount).maxOfOrNull { getLineRight(it) - getLineLeft(it) } ?: 0f
    if (widestLine > size.width + TOLERANCE_PX) problems += "$name: el texto es más ancho que su espacio (${widestLine.toInt()} de ${size.width})"
    val text = layoutInput.text.text.trimEnd()
    val shownUpTo = if (lineCount == 0) 0 else getLineEnd(lineCount - 1, visibleEnd = true)
    if (!shortensWithEllipsis && shownUpTo < text.length) problems += "$name: el texto se corta sin puntos suspensivos"
    if (!shortensWithEllipsis && multiParagraph.height > size.height + TOLERANCE_PX) problems += "$name: el texto se corta en alto"
    if (checkSplitWords) splitWord(text)?.let { word -> problems += "$name: la palabra \"$word\" se parte en dos líneas" }
    return problems
}

/** A word that did not fit its line and was broken between two letters or digits, like `21/11/202` and `5`. */
private fun TextLayoutResult.splitWord(text: String): String? {
    val breaks = (0 until lineCount - 1).map { line -> getLineEnd(line) }
    val splitAt = breaks.firstOrNull { end -> end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit() }
        ?: return null
    val wordStart = text.lastIndexOfAny(charArrayOf(' ', '\n'), startIndex = splitAt - 1) + 1
    val wordEnd = text.indexOfAny(charArrayOf(' ', '\n'), startIndex = splitAt).takeIf { it >= 0 } ?: text.length
    return text.substring(wordStart, wordEnd)
}

private fun Rect.contains(other: Rect) =
    other.left >= left - TOLERANCE_PX && other.top >= top - TOLERANCE_PX &&
        other.right <= right + TOLERANCE_PX && other.bottom <= bottom + TOLERANCE_PX

private fun Rect.short() = "${width.toInt()}×${height.toInt()} en (${left.toInt()}, ${top.toInt()})"

private fun SemanticsNode.textLayout(): TextLayoutResult? {
    val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
    val results = mutableListOf<TextLayoutResult>()
    action(results)
    return results.firstOrNull()
}

private fun SemanticsNode.describe(): String {
    val text = config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
        ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
        ?: config.getOrNull(SemanticsProperties.TestTag)
    return if (text != null) "\"$text\"" else "nodo $id"
}

/**
 * Writes the frame as a PNG when `MAJESTIC_SNAPSHOTS` names a folder, to review the components
 * by eye: `MAJESTIC_SNAPSHOTS=/tmp/majestic ./gradlew :shared:jvmTest --tests '*DesignSystemLayoutTest*'`.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.saveSnapshotIfAsked(name: String, frame: LayoutFrame) {
    val folder = System.getenv("MAJESTIC_SNAPSHOTS")?.takeIf { it.isNotBlank() } ?: return
    val file = File(folder, "$name-${frame.width.value.toInt()}-${(frame.fontScale * 100).toInt()}${if (frame.darkTheme) "-oscuro" else ""}.png")
    file.parentFile.mkdirs()
    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
}
