package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.ds_clear_search
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/**
 * A text field with its label always visible above it. An [error] replaces the [help] and says
 * what happened and how to fix it.
 */
@Composable
fun MajesticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    help: String? = null,
    error: String? = null,
    leadingIcon: DrawableResource? = null,
    prefix: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        error != null -> colors.danger
        focused -> colors.primary
        else -> colors.lineStrong
    }
    val borderWidth = if (error != null || focused) 2.dp else 1.dp

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(text = label, style = typography.caption.copy(fontSize = typography.body.fontSize), color = colors.inkMuted)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth().semantics { if (error != null) error(error) },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = MajesticSizes.control)
                        .background(colors.surfaceRaised, RoundedCornerShape(MajesticRadius.small))
                        .border(borderWidth, borderColor, RoundedCornerShape(MajesticRadius.small))
                        .padding(start = MajesticSpacing.space4, end = if (trailing != null) MajesticSpacing.space1 else MajesticSpacing.space4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
                ) {
                    if (leadingIcon != null) MajesticIcon(icon = leadingIcon, contentDescription = null, tint = colors.inkMuted)
                    if (prefix != null) Text(text = prefix, style = typography.bodyLarge, color = colors.inkMuted)
                    Box(modifier = Modifier.weight(1f).padding(vertical = MajesticSpacing.space3)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = typography.bodyLarge, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        innerTextField()
                    }
                    trailing?.invoke()
                }
            },
        )
        val supporting = error ?: help
        if (supporting != null) {
            Text(text = supporting, style = typography.caption, color = if (error != null) colors.danger else colors.inkMuted)
        }
    }
}

/**
 * A field for pesos: numeric keyboard, `$` prefix and thousands dots while typing (`1.250.000`).
 * It keeps only the digits, so the amount is always whole pesos.
 */
@Composable
fun MoneyField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    help: String? = null,
    error: String? = null,
) {
    MajesticTextField(
        value = digits,
        onValueChange = { typed -> onDigitsChange(typed.filter(Char::isDigit).take(MAX_MONEY_DIGITS)) },
        label = label,
        modifier = modifier,
        help = help,
        error = error,
        prefix = "$",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = ThousandsSeparatorTransformation,
    )
}

/** A search field: magnifying glass, no label, and a button to clear what was typed. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    MajesticTextField(
        value = query,
        onValueChange = onQueryChange,
        label = null,
        placeholder = placeholder,
        leadingIcon = MajesticIcons.search,
        modifier = modifier,
        trailing = if (query.isEmpty()) {
            null
        } else {
            { MajesticIconButton(icon = MajesticIcons.close, contentDescription = stringResource(Res.string.ds_clear_search), onClick = { onQueryChange("") }) }
        },
    )
}

// Up to $999.999.999.999: far from what a cash drawer holds, and far from the limits of Long.
private const val MAX_MONEY_DIGITS = 12

/** Shows `1250000` as `1.250.000`, keeping the cursor on the same digit. */
internal object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = digits.reversed().chunked(3).joinToString(".").reversed()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val digitsAfter = digits.length - offset
                return formatted.length - (digitsAfter + dotsAmong(digitsAfter))
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count(Char::isDigit)

            // The dots that separate the last [count] digits into groups of three.
            private fun dotsAmong(count: Int): Int = if (count <= 0) 0 else (count - 1) / 3
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
