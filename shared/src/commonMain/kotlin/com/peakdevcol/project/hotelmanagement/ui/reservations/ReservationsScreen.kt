package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationStart
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.nav_reservations
import com.peakdevcol.project.hotelmanagement.resources.reservation_room_and_nights
import com.peakdevcol.project.hotelmanagement.resources.reservations_empty_message
import com.peakdevcol.project.hotelmanagement.resources.reservations_empty_search_message
import com.peakdevcol.project.hotelmanagement.resources.reservations_empty_search_title
import com.peakdevcol.project.hotelmanagement.resources.reservations_empty_title
import com.peakdevcol.project.hotelmanagement.resources.reservations_error
import com.peakdevcol.project.hotelmanagement.resources.reservations_new
import com.peakdevcol.project.hotelmanagement.resources.reservations_search
import com.peakdevcol.project.hotelmanagement.resources.reservations_upcoming_hint
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountText
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SearchField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Column
import com.peakdevcol.project.hotelmanagement.resources.reservation_cancelled
import com.peakdevcol.project.hotelmanagement.resources.reservation_marked_no_show
import com.peakdevcol.project.hotelmanagement.resources.reservations_tab_calendar
import com.peakdevcol.project.hotelmanagement.resources.reservations_tab_today
import com.peakdevcol.project.hotelmanagement.resources.reservations_tab_upcoming
import com.peakdevcol.project.hotelmanagement.resources.today_arrivals
import com.peakdevcol.project.hotelmanagement.resources.today_departures
import com.peakdevcol.project.hotelmanagement.resources.today_empty_message
import com.peakdevcol.project.hotelmanagement.resources.today_empty_title
import com.peakdevcol.project.hotelmanagement.resources.today_error
import com.peakdevcol.project.hotelmanagement.resources.today_no_arrivals
import com.peakdevcol.project.hotelmanagement.resources.today_overdue
import com.peakdevcol.project.hotelmanagement.resources.today_overdue_hint
import com.peakdevcol.project.hotelmanagement.resources.today_overdue_subtitle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTabs
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.RoomStatusBadge
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel

/**
 * Decision 2A of M11: the reservations, for reception and the administrators. [onBack] is the
 * arrow back to "Más", when the section was opened from there on the phone.
 */
@Composable
fun ReservationsScreen(
    onNewReservation: (start: ReservationStart?) -> Unit,
    onChangeReservation: (reservationId: String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ReservationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReservationsEffect.NavigateToNewReservation -> onNewReservation(effect.start)
                is ReservationsEffect.NavigateToChange -> onChangeReservation(effect.reservationId)
                is ReservationsEffect.Cancelled -> messages.show(getString(Res.string.reservation_cancelled, effect.guestName))
                is ReservationsEffect.MarkedNoShow -> messages.show(getString(Res.string.reservation_marked_no_show, effect.guestName))
            }
        }
    }

    ReservationsContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun ReservationsContent(state: ReservationsState, onIntent: (ReservationsIntent) -> Unit, onBack: (() -> Unit)? = null) {
    val wide = LocalWideWindow.current
    Scaffold(
        topBar = {
            MajesticTopBar(title = stringResource(Res.string.nav_reservations), onBack = onBack) {
                if (wide) {
                    MajesticButton(
                        text = stringResource(Res.string.reservations_new),
                        onClick = { onIntent(ReservationsIntent.NewReservation) },
                        icon = MajesticIcons.add,
                        modifier = Modifier.padding(end = MajesticSpacing.space3),
                    )
                }
            }
        },
        floatingActionButton = {
            if (!wide) {
                MajesticFloatingActionButton(
                    text = stringResource(Res.string.reservations_new),
                    icon = MajesticIcons.add,
                    onClick = { onIntent(ReservationsIntent.NewReservation) },
                )
            }
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            MajesticTabs(
                tabs = ReservationsTab.entries.map { stringResource(it.label()) },
                selectedIndex = state.tab.ordinal,
                onSelect = { index -> onIntent(ReservationsIntent.SelectTab(ReservationsTab.entries[index])) },
                modifier = Modifier.widthIn(max = MajesticSizes.content),
            )
            // Room for the floating button, so it never covers the last reservation.
            val bottomClearance = if (wide) MajesticSpacing.space4 else MajesticSizes.floatingActionButtonClearance
            val contentModifier = Modifier.widthIn(max = MajesticSizes.content)
            when (state.tab) {
                ReservationsTab.TODAY -> TodayReservationsList(state = state, onIntent = onIntent, bottomClearance = bottomClearance, modifier = contentModifier)
                ReservationsTab.UPCOMING -> UpcomingReservations(state = state, onIntent = onIntent, bottomClearance = bottomClearance, modifier = contentModifier)
                // The grid takes the whole width: more days fit on a wide window.
                ReservationsTab.CALENDAR -> OccupancyCalendarTab(state = state, onIntent = onIntent, bottomClearance = bottomClearance)
            }
        }
    }
    state.openReservation?.let { reservation -> ReservationDetailSheet(reservation = reservation, state = state, onIntent = onIntent) }
}

private fun ReservationsTab.label(): StringResource = when (this) {
    ReservationsTab.TODAY -> Res.string.reservations_tab_today
    ReservationsTab.UPCOMING -> Res.string.reservations_tab_upcoming
    ReservationsTab.CALENDAR -> Res.string.reservations_tab_calendar
}

/** RF-RES-07 and RF-RES-09: the arrivals with their room, the departures and the reservations that did not arrive. */
@Composable
private fun TodayReservationsList(state: ReservationsState, onIntent: (ReservationsIntent) -> Unit, bottomClearance: Dp, modifier: Modifier = Modifier) {
    val today = state.today
    when {
        state.todayFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.today_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(ReservationsIntent.Retry) },
            modifier = modifier,
        )
        today == null -> MajesticLoading(modifier)
        today.isEmpty -> EmptyState(
            icon = MajesticIcons.reservation,
            title = stringResource(Res.string.today_empty_title),
            message = stringResource(Res.string.today_empty_message),
            modifier = modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MajesticSpacing.space4, end = MajesticSpacing.space4, top = MajesticSpacing.space2, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        ) {
            item(key = "arrivals-title") { SectionLabel(text = stringResource(Res.string.today_arrivals)) }
            item(key = "arrivals") {
                if (today.arrivals.isEmpty()) {
                    Hint(stringResource(Res.string.today_no_arrivals))
                } else {
                    ListGroup {
                        today.arrivals.forEachIndexed { index, arrival ->
                            if (index > 0) ListDivider()
                            ListRow(
                                title = arrival.reservation.guestName,
                                subtitle = stringResource(Res.string.reservation_room_and_nights, arrival.reservation.roomNumber, nightsLabel(arrival.reservation.stay.nightCount)),
                                trailing = arrival.roomStatus?.let { status -> { RoomStatusBadge(status = status) } },
                                onClick = { onIntent(ReservationsIntent.OpenReservation(arrival.reservation.id)) },
                            )
                        }
                    }
                }
            }
            if (today.departures.isNotEmpty()) {
                item(key = "departures-title") { SectionLabel(text = stringResource(Res.string.today_departures), modifier = Modifier.padding(top = MajesticSpacing.space2)) }
                item(key = "departures") {
                    ListGroup {
                        today.departures.forEachIndexed { index, reservation ->
                            if (index > 0) ListDivider()
                            ReservationRow(reservation = reservation, onClick = { onIntent(ReservationsIntent.OpenReservation(reservation.id)) })
                        }
                    }
                }
            }
            if (today.overdue.isNotEmpty()) {
                item(key = "overdue-title") { SectionLabel(text = stringResource(Res.string.today_overdue), modifier = Modifier.padding(top = MajesticSpacing.space2)) }
                item(key = "overdue-hint") { Hint(stringResource(Res.string.today_overdue_hint)) }
                item(key = "overdue") {
                    ListGroup {
                        today.overdue.forEachIndexed { index, reservation ->
                            if (index > 0) ListDivider()
                            ListRow(
                                title = reservation.guestName,
                                subtitle = stringResource(Res.string.today_overdue_subtitle, formatWeekday(reservation.stay.checkIn), reservation.roomNumber),
                                leading = { MajesticIcon(icon = MajesticIcons.warning, contentDescription = null, tint = MajesticTheme.colors.warning) },
                                onClick = { onIntent(ReservationsIntent.OpenReservation(reservation.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted, maxLines = 4, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun UpcomingReservations(state: ReservationsState, onIntent: (ReservationsIntent) -> Unit, bottomClearance: Dp, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> MajesticLoading(modifier)
        state.loadFailed -> EmptyState(
            icon = MajesticIcons.error,
            title = stringResource(Res.string.reservations_error),
            message = stringResource(Res.string.error_check_connection),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(ReservationsIntent.Retry) },
            modifier = modifier,
        )
        state.reservations.isEmpty() -> EmptyState(
            icon = MajesticIcons.reservation,
            title = stringResource(Res.string.reservations_empty_title),
            message = stringResource(Res.string.reservations_empty_message),
            actionText = stringResource(Res.string.reservations_new),
            actionIcon = MajesticIcons.add,
            onAction = { onIntent(ReservationsIntent.NewReservation) },
            modifier = modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MajesticSpacing.space4, end = MajesticSpacing.space4, top = MajesticSpacing.space2, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        ) {
            item(key = "search") {
                SearchField(
                    query = state.query,
                    onQueryChange = { onIntent(ReservationsIntent.QueryChange(it)) },
                    placeholder = stringResource(Res.string.reservations_search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "hint") { Hint(stringResource(Res.string.reservations_upcoming_hint)) }
            val days = state.arrivalDays
            if (days.isEmpty()) {
                item(key = "no-match") {
                    EmptyState(
                        icon = MajesticIcons.search,
                        title = stringResource(Res.string.reservations_empty_search_title),
                        message = stringResource(Res.string.reservations_empty_search_message),
                    )
                }
            }
            days.forEach { day ->
                item(key = "day-${day.date.isoText}") {
                    SectionLabel(text = formatWeekday(day.date), modifier = Modifier.padding(top = MajesticSpacing.space2))
                }
                item(key = "reservations-${day.date.isoText}") {
                    ListGroup {
                        day.reservations.forEachIndexed { index, reservation ->
                            if (index > 0) ListDivider()
                            ReservationRow(reservation = reservation, onClick = { onIntent(ReservationsIntent.OpenReservation(reservation.id)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationRow(reservation: Reservation, onClick: () -> Unit) {
    ListRow(
        title = reservation.guestName,
        subtitle = stringResource(Res.string.reservation_room_and_nights, reservation.roomNumber, nightsLabel(reservation.stay.nightCount)),
        trailing = {
            AmountText(
                amount = reservation.total,
                style = MajesticTheme.typography.label.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize, fontFeatureSettings = "tnum"),
            )
        },
        onClick = onClick,
    )
}
