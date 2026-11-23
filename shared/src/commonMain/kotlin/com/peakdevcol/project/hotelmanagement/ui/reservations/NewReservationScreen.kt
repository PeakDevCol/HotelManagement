package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.AvailableRoom
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.MAX_ROOM_CAPACITY
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.formatDayRange
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.change_reservation_done
import com.peakdevcol.project.hotelmanagement.resources.change_reservation_error_title
import com.peakdevcol.project.hotelmanagement.resources.change_reservation_save
import com.peakdevcol.project.hotelmanagement.resources.change_reservation_title
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_adults
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_children
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_dates
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_dates_hint
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_done
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest_change
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest_edit
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest_new
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest_not_found
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_guest_search
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_notes
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_pick_dates
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_rate_and_source
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_rate_fixed
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_rate_hint
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_reserve
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_room
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_room_option
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_room_title
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_rooms_empty
import com.peakdevcol.project.hotelmanagement.resources.new_reservation_rooms_without_dates
import com.peakdevcol.project.hotelmanagement.resources.reservation_nights_times_rate
import com.peakdevcol.project.hotelmanagement.resources.reservation_rate
import com.peakdevcol.project.hotelmanagement.resources.reservation_source
import com.peakdevcol.project.hotelmanagement.resources.reservation_tax
import com.peakdevcol.project.hotelmanagement.resources.reservation_total
import com.peakdevcol.project.hotelmanagement.resources.reservations_new
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Avatar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.CountStepper
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MoneyField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SearchField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.initialsOf
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.DaysRangeDialog
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.rooms.capacityLabel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * RF-RES-01 to RF-RES-03: a new reservation, from the days and the people to the guest. With a
 * [reservationId], the same screen changes that reservation (RF-RES-05).
 */
@Composable
fun NewReservationScreen(
    onBack: () -> Unit,
    reservationId: String? = null,
    viewModel: NewReservationViewModel = koinViewModel(key = reservationId ?: NEW_RESERVATION_KEY) { parametersOf(reservationId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NewReservationEffect.Reserved -> {
                    messages.show(getString(Res.string.new_reservation_done, effect.guestName, effect.roomNumber))
                    onBack()
                }
                is NewReservationEffect.Changed -> {
                    messages.show(getString(Res.string.change_reservation_done, effect.guestName, effect.roomNumber))
                    onBack()
                }
                is NewReservationEffect.GuestFailed -> messages.show(getString(effect.error.message()))
            }
        }
    }

    NewReservationContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun NewReservationContent(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            MajesticTopBar(title = stringResource(if (state.isChange) Res.string.change_reservation_title else Res.string.reservations_new), onBack = onBack)
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        val loadError = state.loadError
        if (state.isStarting) {
            MajesticLoading(Modifier.padding(padding))
        } else if (loadError != null) {
            EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.change_reservation_error_title),
                message = loadError.text(),
                modifier = Modifier.padding(padding),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                NewReservationForm(state = state, onIntent = onIntent)
            }
        }
    }
    val today = state.today
    if (state.isPickingDates && today != null) {
        val stay = state.stay
        DaysRangeDialog(
            firstDay = stay?.checkIn ?: today,
            lastDay = stay?.checkOut ?: today.plusDays(1),
            onConfirm = { first, last -> onIntent(NewReservationIntent.DatesChosen(first, last)) },
            onDismiss = { onIntent(NewReservationIntent.DismissDates) },
            title = Res.string.new_reservation_pick_dates,
            firstSelectableDay = today,
            differentDays = true,
        )
    }
    state.guestForm?.let { form -> GuestFormSheet(form = form, isSaving = state.isSavingGuest, onIntent = onIntent) }
}

@Composable
private fun NewReservationForm(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = MajesticSizes.content)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(MajesticSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        SectionLabel(text = stringResource(Res.string.new_reservation_dates))
        StayAndPeople(state = state, onIntent = onIntent)
        SectionLabel(text = stringResource(Res.string.new_reservation_room), modifier = Modifier.padding(top = MajesticSpacing.space2))
        RoomChoice(state = state, onIntent = onIntent)
        SectionLabel(text = stringResource(Res.string.new_reservation_guest), modifier = Modifier.padding(top = MajesticSpacing.space2))
        GuestChoice(state = state, onIntent = onIntent)
        SectionLabel(text = stringResource(Res.string.new_reservation_rate_and_source), modifier = Modifier.padding(top = MajesticSpacing.space2))
        RateAndSource(state = state, onIntent = onIntent)
        Summary(state = state, onIntent = onIntent)
    }
}

@Composable
private fun StayAndPeople(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit) {
    val stay = state.stay
    SectionCard(help = stringResource(Res.string.new_reservation_dates_hint)) {
        MajesticButton(
            text = stay?.let { formatDayRange(it.checkIn, it.checkOut) } ?: stringResource(Res.string.new_reservation_pick_dates),
            onClick = { onIntent(NewReservationIntent.PickDates) },
            style = if (stay == null) ButtonStyle.Primary else ButtonStyle.Outline,
            icon = MajesticIcons.calendar,
            modifier = Modifier.fillMaxWidth(),
        )
        stay?.let { Text(text = nightsLabel(it.nightCount), style = MajesticTheme.typography.label, color = MajesticTheme.colors.ink, maxLines = 1) }
        CountStepper(
            label = stringResource(Res.string.new_reservation_adults),
            count = state.adults,
            onCountChange = { onIntent(NewReservationIntent.AdultsChange(it)) },
            minimum = 1,
            maximum = MAX_ROOM_CAPACITY,
        )
        CountStepper(
            label = stringResource(Res.string.new_reservation_children),
            count = state.children,
            onCountChange = { onIntent(NewReservationIntent.ChildrenChange(it)) },
            maximum = MAX_ROOM_CAPACITY - 1,
        )
    }
}

/** RF-RES-03: the free rooms for the days and the people, each with its rate. */
@Composable
private fun RoomChoice(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit) {
    when (val search = state.roomSearch) {
        RoomSearch.NotAsked -> Hint(stringResource(Res.string.new_reservation_rooms_without_dates))
        RoomSearch.Searching -> MajesticLoading(Modifier.fillMaxWidth().height(MajesticSizes.controlLarge))
        is RoomSearch.Failed -> Banner(
            tone = BannerTone.Danger,
            message = search.error.text(),
            actionText = stringResource(Res.string.retry),
            onAction = { onIntent(NewReservationIntent.RetryRooms) },
        )
        is RoomSearch.Found -> if (search.rooms.isEmpty()) {
            Hint(stringResource(Res.string.new_reservation_rooms_empty))
        } else {
            ListGroup {
                search.rooms.forEachIndexed { index, room ->
                    if (index > 0) ListDivider()
                    RoomOption(room = room, selected = room.room.id == state.roomId, onClick = { onIntent(NewReservationIntent.SelectRoom(room.room.id)) })
                }
            }
        }
    }
}

@Composable
private fun RoomOption(room: AvailableRoom, selected: Boolean, onClick: () -> Unit) {
    ListRow(
        title = stringResource(Res.string.new_reservation_room_title, room.room.number, room.type.name),
        subtitle = stringResource(Res.string.new_reservation_room_option, capacityLabel(room.type.capacity), formatPesos(room.nightlyRate)),
        leading = { MajesticIcon(icon = if (selected) MajesticIcons.roomFilled else MajesticIcons.room, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        trailing = if (selected) ({ MajesticIcon(icon = MajesticIcons.checkCircle, contentDescription = null, tint = MajesticTheme.colors.primary) }) else null,
        onClick = onClick,
    )
}

/** RF-RES-01: the guest is searched by name or document, or created with the name and the phone (decision 5A). */
@Composable
private fun GuestChoice(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit) {
    val guest = state.guest
    if (guest != null) {
        ChosenGuest(guest = guest, canChange = !state.isChange, onIntent = onIntent)
        return
    }
    SearchField(
        query = state.guestQuery,
        onQueryChange = { onIntent(NewReservationIntent.GuestQueryChange(it)) },
        placeholder = stringResource(Res.string.new_reservation_guest_search),
        modifier = Modifier.fillMaxWidth(),
    )
    when (val search = state.guestSearch) {
        GuestSearch.Idle -> Unit
        GuestSearch.Searching -> MajesticLoading(Modifier.fillMaxWidth().height(MajesticSizes.controlLarge))
        is GuestSearch.Failed -> Banner(tone = BannerTone.Danger, message = stringResource(search.error.message()))
        is GuestSearch.Found -> if (search.guests.isEmpty()) {
            Hint(stringResource(Res.string.new_reservation_guest_not_found))
        } else {
            ListGroup {
                search.guests.forEachIndexed { index, found ->
                    if (index > 0) ListDivider()
                    ListRow(
                        title = found.fullName,
                        subtitle = found.identityLabel(),
                        leading = { Avatar(initials = initialsOf(found.fullName)) },
                        onClick = { onIntent(NewReservationIntent.SelectGuest(found.id)) },
                    )
                }
            }
        }
    }
    MajesticButton(
        text = stringResource(Res.string.new_reservation_guest_new),
        onClick = { onIntent(NewReservationIntent.NewGuest) },
        style = ButtonStyle.Outline,
        icon = MajesticIcons.add,
        modifier = Modifier.fillMaxWidth(),
    )
}

// Decision 2A of the second PR of M11: the guest of a reservation being changed stays.
@Composable
private fun ChosenGuest(guest: Guest, canChange: Boolean, onIntent: (NewReservationIntent) -> Unit) {
    ListGroup {
        ListRow(
            title = guest.fullName,
            subtitle = guest.identityLabel(),
            leading = { Avatar(initials = initialsOf(guest.fullName)) },
            trailing = if (canChange) {
                {
                    MajesticButton(
                        text = stringResource(Res.string.new_reservation_guest_change),
                        onClick = { onIntent(NewReservationIntent.ChangeGuest) },
                        style = ButtonStyle.Text,
                    )
                }
            } else {
                null
            },
        )
    }
    MajesticButton(
        text = stringResource(Res.string.new_reservation_guest_edit),
        onClick = { onIntent(NewReservationIntent.EditGuest) },
        style = ButtonStyle.Text,
        icon = MajesticIcons.edit,
    )
}

/** RF-RES-02: where the reservation came from and, for an administrator, the rate of each night. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateAndSource(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            FieldLabel(text = stringResource(Res.string.reservation_source))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                ReservationSource.entries.forEach { source ->
                    MajesticChip(text = stringResource(source.label()), selected = state.source == source, onClick = { onIntent(NewReservationIntent.SourceSelect(source)) })
                }
            }
        }
        state.selectedRoom?.let { room ->
            if (state.canChangeRate) {
                MoneyField(
                    digits = state.rateText,
                    onDigitsChange = { onIntent(NewReservationIntent.RateChange(it)) },
                    label = stringResource(Res.string.reservation_rate),
                    help = stringResource(Res.string.new_reservation_rate_hint, room.type.name, formatPesos(room.nightlyRate)),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Hint(stringResource(Res.string.new_reservation_rate_fixed, formatPesos(room.nightlyRate)))
            }
        }
        MajesticTextField(
            value = state.notes,
            onValueChange = { onIntent(NewReservationIntent.NotesChange(it)) },
            label = stringResource(Res.string.new_reservation_notes),
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** What the stay costs, with the tax it includes, and the button that reserves it. */
@Composable
private fun Summary(state: NewReservationState, onIntent: (NewReservationIntent) -> Unit) {
    SectionCard {
        state.price?.let { price ->
            AmountRow(
                label = stringResource(Res.string.reservation_nights_times_rate, nightsLabel(price.nights), formatPesos(price.nightlyRate)),
                amount = price.total,
            )
            if (price.taxAmount > 0) AmountRow(label = stringResource(Res.string.reservation_tax), amount = price.taxAmount)
            ListDivider()
            AmountRow(label = stringResource(Res.string.reservation_total), amount = price.total, emphasized = true)
        }
        state.error?.let { error -> Banner(tone = BannerTone.Danger, message = error.text()) }
        MajesticButton(
            text = stringResource(if (state.isChange) Res.string.change_reservation_save else Res.string.new_reservation_reserve),
            onClick = { onIntent(NewReservationIntent.Reserve) },
            size = ButtonSize.Large,
            icon = MajesticIcons.reservation,
            enabled = state.canReserve,
            loading = state.isReserving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

// One screen for a new reservation, apart from the one of each reservation being changed.
private const val NEW_RESERVATION_KEY = "new-reservation"
