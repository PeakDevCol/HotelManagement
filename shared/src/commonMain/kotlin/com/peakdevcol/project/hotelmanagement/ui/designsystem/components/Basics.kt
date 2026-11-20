package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.ds_hotel_name
import com.peakdevcol.project.hotelmanagement.resources.hotel_logo
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** An icon of the system, tinted with the color of the content around it unless one is given. */
@Composable
fun MajesticIcon(
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = MajesticSizes.icon,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/**
 * A figure that never wraps and is never cut: it shrinks from the size of [style] down to
 * [minFontSize] to fit its width. The layout around it keeps enough room for the minimum
 * (RNF-18, "Contenido que no cabe").
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 20.sp,
    textAlign: TextAlign = TextAlign.Start,
) {
    val contentColor = LocalContentColor.current
    val resolvedColor = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        else -> contentColor
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolvedColor, textAlign = textAlign),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = style.fontSize, stepSize = 1.sp),
    )
}

/** An amount in pesos, as the hotel writes it (`$24.500`), that shrinks to fit (see [AutoSizeText]). */
@Composable
fun AmountText(
    amount: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MajesticTheme.typography.amount,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 16.sp,
    textAlign: TextAlign = TextAlign.End,
) {
    AutoSizeText(
        text = formatPesos(amount),
        style = style,
        modifier = modifier,
        color = color,
        minFontSize = minFontSize,
        textAlign = textAlign,
    )
}

/** An upper-case label above a figure or a group. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MajesticTheme.colors.inkMuted) {
    Text(
        text = text.uppercase(),
        style = MajesticTheme.typography.overline,
        color = color,
        modifier = modifier,
    )
}

/** A label and an amount on one line, used in sheets, details and totals. */
@Composable
fun AmountRow(label: String, amount: Long, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val typography = MajesticTheme.typography
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (emphasized) typography.title else typography.bodyLarge,
            color = MajesticTheme.colors.ink,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        AmountText(
            amount = amount,
            style = if (emphasized) typography.headline.copy(fontFeatureSettings = "tnum") else typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = MajesticTheme.colors.ink,
        )
    }
}

/** A hairline between rows of a group. */
@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = MajesticTheme.colors.line)
}

/** The hotel logo on its light plate, which keeps it visible on dark grounds. */
@Composable
fun HotelLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(MajesticTheme.colors.logoGround, RoundedCornerShape(if (size > 56.dp) MajesticRadius.large else MajesticRadius.medium)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.hotel_logo),
            contentDescription = stringResource(Res.string.ds_hotel_name),
            modifier = Modifier.size(size * 0.62f),
        )
    }
}

/** The hotel as the login presents it: the logo on its plate, its name and the brass rule. */
@Composable
fun HotelBrandHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4)) {
        HotelLogo(size = 72.dp)
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            SectionLabel(text = stringResource(Res.string.ds_hotel_name))
            Box(modifier = Modifier.size(width = 32.dp, height = 2.dp).background(MajesticTheme.colors.brass))
        }
    }
}
