package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyCalendar
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancyPeriod
import com.peakdevcol.project.hotelmanagement.domain.model.OccupancySpan
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.domain.model.formatDayRange
import com.peakdevcol.project.hotelmanagement.domain.model.formatShortWeekday
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.calendar_bar_description
import com.peakdevcol.project.hotelmanagement.resources.calendar_error
import com.peakdevcol.project.hotelmanagement.resources.calendar_free_night
import com.peakdevcol.project.hotelmanagement.resources.calendar_hint
import com.peakdevcol.project.hotelmanagement.resources.calendar_month
import com.peakdevcol.project.hotelmanagement.resources.calendar_next
import com.peakdevcol.project.hotelmanagement.resources.calendar_no_rooms_message
import com.peakdevcol.project.hotelmanagement.resources.calendar_no_rooms_title
import com.peakdevcol.project.hotelmanagement.resources.calendar_previous
import com.peakdevcol.project.hotelmanagement.resources.calendar_today
import com.peakdevcol.project.hotelmanagement.resources.calendar_two_weeks
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OccupancyBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OccupancyDay
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OccupancyGrid
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OccupancyRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.OccupancyTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SegmentedControl
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.stringResource

/** RF-RES-04: the rooms by day, in two weeks or a month, with arrows and "Hoy". */
@Composable
internal fun OccupancyCalendarTab(state: ReservationsState, onIntent: (ReservationsIntent) -> Unit, bottomClearance: Dp, modifier: Modifier = Modifier) {
    val period = state.calendarPeriod
    val today = state.calendarToday
    if (period == null || today == null) {
        MajesticLoading(modifier)
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        PeriodBar(period = period, onIntent = onIntent)
        val occupancy = state.occupancy
        when {
            state.occupancyFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.calendar_error),
                message = stringResource(Res.string.error_check_connection),
                actionText = stringResource(Res.string.retry),
                onAction = { onIntent(ReservationsIntent.Retry) },
            )
            occupancy == null -> MajesticLoading()
            occupancy.rooms.isEmpty() -> EmptyState(
                icon = MajesticIcons.room,
                title = stringResource(Res.string.calendar_no_rooms_title),
                message = stringResource(Res.string.calendar_no_rooms_message),
            )
            else -> OccupancyGridOf(occupancy = occupancy, today = today, onIntent = onIntent, modifier = Modifier.padding(bottom = bottomClearance))
        }
    }
}

@Composable
private fun PeriodBar(period: OccupancyPeriod, onIntent: (ReservationsIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MajesticSpacing.space4, vertical = MajesticSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MajesticIconButton(icon = MajesticIcons.chevronLeft, contentDescription = stringResource(Res.string.calendar_previous), onClick = { onIntent(ReservationsIntent.PreviousPeriod) })
            Text(
                text = formatDayRange(period.firstDay, period.lastDay),
                style = MajesticTheme.typography.title,
                color = MajesticTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            MajesticIconButton(icon = MajesticIcons.chevronRight, contentDescription = stringResource(Res.string.calendar_next), onClick = { onIntent(ReservationsIntent.NextPeriod) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalAlignment = Alignment.CenterVertically) {
            SegmentedControl(
                options = OccupancySpan.entries,
                selected = period.span,
                onSelect = { onIntent(ReservationsIntent.SelectSpan(it)) },
                label = { span -> stringResource(if (span == OccupancySpan.TWO_WEEKS) Res.string.calendar_two_weeks else Res.string.calendar_month) },
                modifier = Modifier.weight(1f),
            )
            MajesticButton(text = stringResource(Res.string.calendar_today), onClick = { onIntent(ReservationsIntent.CalendarToday) }, style = ButtonStyle.Outline)
        }
        Text(
            text = stringResource(Res.string.calendar_hint),
            style = MajesticTheme.typography.caption,
            color = MajesticTheme.colors.inkMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OccupancyGridOf(occupancy: OccupancyCalendar, today: CalendarDate, onIntent: (ReservationsIntent) -> Unit, modifier: Modifier = Modifier) {
    val period = occupancy.period
    val days = period.days
    val rows = occupancy.rooms.map { room ->
        OccupancyRow(
            id = room.room.id,
            title = room.room.number.toString(),
            subtitle = room.typeName,
            bars = room.reservations.map { barOf(it, period) },
        )
    }
    val numbers = occupancy.rooms.associate { it.room.id to it.room.number }
    OccupancyGrid(
        days = days.map { day -> OccupancyDay(weekday = formatShortWeekday(day), day = day.day.toString(), isToday = day == today, isPast = day < today) },
        rows = rows,
        onBarClick = { reservationId -> onIntent(ReservationsIntent.OpenReservation(reservationId)) },
        onFreeNightClick = { roomId, column -> onIntent(ReservationsIntent.ReserveNight(roomId, days[column])) },
        freeNightDescription = { row, column -> stringResource(Res.string.calendar_free_night, numbers.getValue(row.id), formatWeekday(days[column])) },
        modifier = modifier,
        // Today in sight, with the night before it, when it is in the period.
        firstShownColumn = (days.indexOf(today) - 1).coerceAtLeast(0),
    )
}

/** The nights of the reservation inside the period, as columns; straight where it goes on outside. */
@Composable
private fun barOf(reservation: Reservation, period: OccupancyPeriod): OccupancyBar {
    val stay = reservation.stay
    val lastNight = stay.checkOut.plusDays(-1)
    return OccupancyBar(
        id = reservation.id,
        label = reservation.guestName,
        description = stringResource(Res.string.calendar_bar_description, reservation.guestName, formatDayRange(stay.checkIn, stay.checkOut)),
        firstColumn = (maxOf(stay.checkIn, period.firstDay).epochDay - period.firstDay.epochDay).toInt(),
        lastColumn = (minOf(lastNight, period.lastDay).epochDay - period.firstDay.epochDay).toInt(),
        tone = if (reservation.state == ReservationState.CHECKED_IN) OccupancyTone.InHouse else OccupancyTone.Confirmed,
        continuesBefore = stay.checkIn < period.firstDay,
        continuesAfter = lastNight > period.lastDay,
    )
}
