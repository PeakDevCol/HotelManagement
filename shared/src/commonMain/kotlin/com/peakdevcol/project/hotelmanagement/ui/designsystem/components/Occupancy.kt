package com.peakdevcol.project.hotelmanagement.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticRadius
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme

/** A column of the occupancy grid: one day. */
data class OccupancyDay(
    /** "lun". */
    val weekday: String,
    /** "22". */
    val day: String,
    val isToday: Boolean = false,
    /** A night that is over cannot be reserved. */
    val isPast: Boolean = false,
)

/** How a bar reads: a stay to come, or a guest who is in the hotel. */
enum class OccupancyTone {
    Confirmed,
    InHouse,
}

/** Something that holds the nights of a row from [firstColumn] to [lastColumn], both included. */
data class OccupancyBar(
    val id: String,
    val label: String,
    /** What a screen reader says: who and which days. */
    val description: String,
    val firstColumn: Int,
    val lastColumn: Int,
    val tone: OccupancyTone,
    /** It started before the first day shown, so its start is straight. */
    val continuesBefore: Boolean = false,
    /** It goes on after the last day shown, so its end is straight. */
    val continuesAfter: Boolean = false,
)

/** A row of the occupancy grid: a room and what holds its nights. */
data class OccupancyRow(
    val id: String,
    /** "1204". */
    val title: String,
    /** The type of the room. */
    val subtitle: String?,
    val bars: List<OccupancyBar>,
)

private val NightWidth = MajesticSizes.control
private val RowHeight = MajesticSizes.controlLarge
private val RoomColumnWidth = 96.dp

/**
 * RF-RES-04: rooms by day, each stay as a bar. The rooms stay on the left and the days slide
 * sideways, starting at [firstShownColumn], like today; every night and every bar is at least
 * 48 dp, so each one is touched. A free night that is not over calls [onFreeNightClick];
 * [freeNightDescription] says which, for screen readers.
 */
@Composable
fun OccupancyGrid(
    days: List<OccupancyDay>,
    rows: List<OccupancyRow>,
    onBarClick: (barId: String) -> Unit,
    onFreeNightClick: (rowId: String, column: Int) -> Unit,
    freeNightDescription: @Composable (row: OccupancyRow, column: Int) -> String,
    modifier: Modifier = Modifier,
    firstShownColumn: Int = 0,
) {
    val colors = MajesticTheme.colors
    val nightsScroll = rememberScrollState()
    val density = LocalDensity.current
    // Again when the room for the days changes, like turning the phone, so the column stays in sight.
    LaunchedEffect(days, firstShownColumn, nightsScroll.maxValue) {
        nightsScroll.scrollTo(with(density) { (NightWidth * firstShownColumn).roundToPx() })
    }
    Row(modifier = modifier.verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.width(RoomColumnWidth)) {
            Box(Modifier.size(RoomColumnWidth, RowHeight).border(0.5.dp, colors.line))
            rows.forEach { row -> RoomCell(row) }
        }
        Column(modifier = Modifier.horizontalScroll(nightsScroll)) {
            Row { days.forEach { day -> DayHeader(day) } }
            rows.forEach { row -> NightsRow(row, days, onBarClick, onFreeNightClick, freeNightDescription) }
        }
    }
}

@Composable
private fun RoomCell(row: OccupancyRow) {
    val colors = MajesticTheme.colors
    Column(
        modifier = Modifier
            .size(RoomColumnWidth, RowHeight)
            .border(0.5.dp, colors.line)
            .padding(horizontal = MajesticSpacing.space2),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = row.title, style = MajesticTheme.typography.label, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        row.subtitle?.let { Text(text = it, style = MajesticTheme.typography.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun DayHeader(day: OccupancyDay) {
    val colors = MajesticTheme.colors
    val ink = if (day.isToday) colors.primary else colors.inkMuted
    Column(
        modifier = Modifier
            .size(NightWidth, RowHeight)
            .background(dayGround(day))
            .border(0.5.dp, colors.line),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = day.weekday, style = MajesticTheme.typography.caption, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(text = day.day, style = MajesticTheme.typography.label, color = if (day.isToday) colors.primary else colors.ink, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun dayGround(day: OccupancyDay): Color {
    val colors = MajesticTheme.colors
    return when {
        day.isToday -> colors.stateHover
        day.isPast -> colors.surfaceSunken
        else -> colors.surfaceRaised
    }
}

@Composable
private fun NightsRow(
    row: OccupancyRow,
    days: List<OccupancyDay>,
    onBarClick: (String) -> Unit,
    onFreeNightClick: (String, Int) -> Unit,
    freeNightDescription: @Composable (OccupancyRow, Int) -> String,
) {
    val held = row.bars.flatMap { it.firstColumn..it.lastColumn }.toSet()
    Box(modifier = Modifier.width(NightWidth * days.size).height(RowHeight)) {
        Row {
            days.forEachIndexed { column, day ->
                val free = column !in held && !day.isPast
                val description = if (free) freeNightDescription(row, column) else null
                Box(
                    modifier = Modifier
                        .size(NightWidth, RowHeight)
                        .background(dayGround(day))
                        .border(0.5.dp, MajesticTheme.colors.line)
                        .then(
                            if (free) {
                                Modifier
                                    .semantics { contentDescription = description.orEmpty() }
                                    .clickable { onFreeNightClick(row.id, column) }
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
        row.bars.forEach { bar -> Bar(bar, onClick = { onBarClick(bar.id) }) }
    }
}

// The whole height of the row and every night it holds respond to the touch; the drawn bar is inset.
@Composable
private fun Bar(bar: OccupancyBar, onClick: () -> Unit) {
    val colors = MajesticTheme.colors
    val (ground, ink) = when (bar.tone) {
        OccupancyTone.Confirmed -> colors.primarySoft to colors.onPrimarySoft
        OccupancyTone.InHouse -> colors.primary to colors.onPrimary
    }
    val radius = MajesticRadius.small
    val shape = RoundedCornerShape(
        topStart = if (bar.continuesBefore) 0.dp else radius,
        bottomStart = if (bar.continuesBefore) 0.dp else radius,
        topEnd = if (bar.continuesAfter) 0.dp else radius,
        bottomEnd = if (bar.continuesAfter) 0.dp else radius,
    )
    Box(
        modifier = Modifier
            .offset(x = NightWidth * bar.firstColumn)
            .width(NightWidth * (bar.lastColumn - bar.firstColumn + 1))
            .fillMaxHeight()
            .semantics { contentDescription = bar.description }
            .clickable(onClick = onClick)
            .padding(vertical = MajesticSpacing.space2, horizontal = 2.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(ground, shape).padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = bar.label, style = MajesticTheme.typography.caption, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
