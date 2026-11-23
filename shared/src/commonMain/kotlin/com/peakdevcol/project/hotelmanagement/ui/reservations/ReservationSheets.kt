package com.peakdevcol.project.hotelmanagement.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.Reservation
import com.peakdevcol.project.hotelmanagement.domain.model.formatWeekday
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.destination_room_number
import com.peakdevcol.project.hotelmanagement.resources.guest_form_document_hint
import com.peakdevcol.project.hotelmanagement.resources.guest_form_document_number
import com.peakdevcol.project.hotelmanagement.resources.guest_form_document_type
import com.peakdevcol.project.hotelmanagement.resources.guest_form_edit_title
import com.peakdevcol.project.hotelmanagement.resources.guest_form_email
import com.peakdevcol.project.hotelmanagement.resources.guest_form_name
import com.peakdevcol.project.hotelmanagement.resources.guest_form_nationality
import com.peakdevcol.project.hotelmanagement.resources.guest_form_new_title
import com.peakdevcol.project.hotelmanagement.resources.guest_form_phone
import com.peakdevcol.project.hotelmanagement.resources.reservation_notes
import com.peakdevcol.project.hotelmanagement.resources.reservation_rate
import com.peakdevcol.project.hotelmanagement.resources.reservation_source
import com.peakdevcol.project.hotelmanagement.resources.reservation_stay
import com.peakdevcol.project.hotelmanagement.resources.reservation_tax
import com.peakdevcol.project.hotelmanagement.resources.reservation_total
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import androidx.compose.foundation.layout.height
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationEventType
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationField
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationHistoryEntry
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSnapshot
import com.peakdevcol.project.hotelmanagement.domain.model.changedFields
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatDayRange
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.reservation_cancel
import com.peakdevcol.project.hotelmanagement.resources.reservation_cancel_back
import com.peakdevcol.project.hotelmanagement.resources.reservation_cancel_reason
import com.peakdevcol.project.hotelmanagement.resources.reservation_change
import com.peakdevcol.project.hotelmanagement.resources.reservation_history
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_cancelled
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_created
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_days
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_error
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_modified
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_no_show
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_notes
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_people
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_rate
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_reason
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_room
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_someone
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_source
import com.peakdevcol.project.hotelmanagement.resources.reservation_history_total
import com.peakdevcol.project.hotelmanagement.resources.reservation_no_show
import com.peakdevcol.project.hotelmanagement.resources.reservation_no_show_hint
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReservationDetailSheet(reservation: Reservation, state: ReservationsState, onIntent: (ReservationsIntent) -> Unit) {
    TaskSheet(
        title = reservation.guestName,
        subtitle = stringResource(Res.string.destination_room_number, reservation.roomNumber),
        onDismiss = { onIntent(ReservationsIntent.CloseReservation) },
        asDialog = LocalWideWindow.current,
    ) {
        ReservationDetailContent(reservation = reservation, state = state, onIntent = onIntent)
    }
}

/** The detail of a reservation, what can be done with it and its history (decision 1A). */
@Composable
fun ReservationDetailContent(reservation: Reservation, state: ReservationsState, onIntent: (ReservationsIntent) -> Unit) {
    ReservationDetailFields(reservation)
    state.closeError?.let { error -> Banner(tone = BannerTone.Danger, message = error.text()) }
    val reason = state.cancelReason
    when {
        reason != null -> CancelForm(reason = reason, isClosing = state.isClosing, onIntent = onIntent)
        state.canChangeOpenReservation -> ReservationActions(state = state, onIntent = onIntent)
    }
    ReservationHistorySection(reservation = reservation, history = state.history)
}

/** RF-RES-05 to RF-RES-07. "No llegó" goes first when the day of the arrival is over, since that is what is pending. */
@Composable
private fun ReservationActions(state: ReservationsState, onIntent: (ReservationsIntent) -> Unit) {
    if (state.canMarkOpenAsNoShow) {
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            MajesticButton(
                text = stringResource(Res.string.reservation_no_show),
                onClick = { onIntent(ReservationsIntent.MarkNoShow) },
                size = ButtonSize.Large,
                icon = MajesticIcons.block,
                loading = state.isClosing,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(text = stringResource(Res.string.reservation_no_show_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        MajesticButton(
            text = stringResource(Res.string.reservation_change),
            onClick = { onIntent(ReservationsIntent.ChangeReservation) },
            style = ButtonStyle.Outline,
            icon = MajesticIcons.edit,
        )
        MajesticButton(
            text = stringResource(Res.string.reservation_cancel),
            onClick = { onIntent(ReservationsIntent.StartCancel) },
            style = ButtonStyle.Text,
            icon = MajesticIcons.close,
        )
    }
}

/** RF-RES-06: a cancellation keeps its reason. */
@Composable
private fun CancelForm(reason: String, isClosing: Boolean, onIntent: (ReservationsIntent) -> Unit) {
    MajesticTextField(
        value = reason,
        onValueChange = { onIntent(ReservationsIntent.CancelReasonChange(it)) },
        label = stringResource(Res.string.reservation_cancel_reason),
        singleLine = false,
        minLines = 2,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End)) {
        MajesticButton(text = stringResource(Res.string.reservation_cancel_back), onClick = { onIntent(ReservationsIntent.DismissCancel) }, style = ButtonStyle.Text)
        MajesticButton(
            text = stringResource(Res.string.reservation_cancel),
            onClick = { onIntent(ReservationsIntent.ConfirmCancel) },
            style = ButtonStyle.Danger,
            enabled = reason.isNotBlank(),
            loading = isClosing,
        )
    }
}

/** Decision 1A: who created the reservation and each change, from the oldest. */
@Composable
private fun ReservationHistorySection(reservation: Reservation, history: HistoryLoad?) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = stringResource(Res.string.reservation_history))
        when (history) {
            null, HistoryLoad.Loading -> MajesticLoading(Modifier.fillMaxWidth().height(MajesticSizes.control))
            HistoryLoad.Failed -> Text(text = stringResource(Res.string.reservation_history_error), style = typography.body, color = colors.danger)
            is HistoryLoad.Loaded -> {
                reservation.createdAt?.let { at ->
                    HistoryLine(stringResource(Res.string.reservation_history_created, history.history.createdByName ?: stringResource(Res.string.reservation_history_someone), formatBogotaDateTime(at)))
                }
                history.history.entries.forEach { entry -> HistoryEntry(entry) }
            }
        }
    }
}

@Composable
private fun HistoryEntry(entry: ReservationHistoryEntry) {
    val event = entry.event
    val who = entry.byName ?: stringResource(Res.string.reservation_history_someone)
    val at = event.at?.let(::formatBogotaDateTime).orEmpty()
    val title = when (event.type) {
        ReservationEventType.MODIFIED -> Res.string.reservation_history_modified
        ReservationEventType.CANCELLED -> Res.string.reservation_history_cancelled
        ReservationEventType.NO_SHOW -> Res.string.reservation_history_no_show
    }
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1)) {
        HistoryLine(stringResource(title, who, at))
        event.reason?.let { HistoryDetail(stringResource(Res.string.reservation_history_reason, it)) }
        val before = event.before
        val after = event.after
        if (before != null && after != null) {
            event.changedFields.forEach { field -> HistoryDetail(changeLabel(field, before, after)) }
        }
    }
}

@Composable
private fun changeLabel(field: ReservationField, before: ReservationSnapshot, after: ReservationSnapshot): String = when (field) {
    ReservationField.DAYS -> stringResource(
        Res.string.reservation_history_days,
        formatDayRange(before.stay.checkIn, before.stay.checkOut),
        formatDayRange(after.stay.checkIn, after.stay.checkOut),
    )
    ReservationField.ROOM -> stringResource(Res.string.reservation_history_room, before.roomNumber, after.roomNumber)
    ReservationField.PEOPLE -> stringResource(Res.string.reservation_history_people, peopleLabel(before.adults, before.children), peopleLabel(after.adults, after.children))
    ReservationField.RATE -> stringResource(Res.string.reservation_history_rate, formatPesos(before.nightlyRate), formatPesos(after.nightlyRate))
    ReservationField.TOTAL -> stringResource(Res.string.reservation_history_total, formatPesos(before.total), formatPesos(after.total))
    ReservationField.SOURCE -> stringResource(Res.string.reservation_history_source, stringResource(before.source.label()), stringResource(after.source.label()))
    ReservationField.NOTES -> stringResource(Res.string.reservation_history_notes)
}

@Composable
private fun HistoryLine(text: String) {
    Text(text = text, style = MajesticTheme.typography.body, color = MajesticTheme.colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun HistoryDetail(text: String) {
    Text(text = text, style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted, maxLines = MAX_NOTES_LINES, overflow = TextOverflow.Ellipsis)
}

/** RF-RES-02: the days, the people, what the stay costs and how it reached the hotel. */
@Composable
fun ReservationDetailFields(reservation: Reservation) {
    val colors = MajesticTheme.colors
    val typography = MajesticTheme.typography
    val stay = reservation.stay
    Text(
        text = stringResource(Res.string.reservation_stay, formatWeekday(stay.checkIn), formatWeekday(stay.checkOut)),
        style = typography.bodyLarge,
        color = colors.ink,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = "${nightsLabel(stay.nightCount)} · ${peopleLabel(reservation.adults, reservation.children)}",
        style = typography.body,
        color = colors.inkMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        reservation.nightlyRates.distinct().singleOrNull()?.let { rate -> AmountRow(label = stringResource(Res.string.reservation_rate), amount = rate) }
        if (reservation.taxAmount > 0) AmountRow(label = stringResource(Res.string.reservation_tax), amount = reservation.taxAmount)
        AmountRow(label = stringResource(Res.string.reservation_total), amount = reservation.total, emphasized = true)
    }
    Text(
        text = "${stringResource(Res.string.reservation_source)}: ${stringResource(reservation.source.label())}",
        style = typography.body,
        color = colors.inkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (reservation.notes.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1)) {
            FieldLabel(text = stringResource(Res.string.reservation_notes))
            Text(text = reservation.notes, style = typography.body, color = colors.ink, maxLines = MAX_NOTES_LINES, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun GuestFormSheet(form: GuestForm, isSaving: Boolean, onIntent: (NewReservationIntent) -> Unit) {
    TaskSheet(
        title = stringResource(if (form.existing == null) Res.string.guest_form_new_title else Res.string.guest_form_edit_title),
        onDismiss = { onIntent(NewReservationIntent.DismissGuestForm) },
        asDialog = LocalWideWindow.current,
    ) {
        GuestFormFields(form = form, isSaving = isSaving, onIntent = onIntent)
    }
}

/**
 * RF-RES-01 with decision 5A: the name and the phone, and the document, the nationality and the
 * email when the guest gives them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuestFormFields(form: GuestForm, isSaving: Boolean, onIntent: (NewReservationIntent) -> Unit) {
    fun errorFor(vararg errors: GuestError): GuestError? = form.error.takeIf { it in errors }
    MajesticTextField(
        value = form.fullName,
        onValueChange = { onIntent(NewReservationIntent.GuestNameChange(it)) },
        label = stringResource(Res.string.guest_form_name),
        error = errorFor(GuestError.BlankName)?.let { stringResource(it.message()) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    MajesticTextField(
        value = form.phone,
        onValueChange = { onIntent(NewReservationIntent.GuestPhoneChange(it)) },
        label = stringResource(Res.string.guest_form_phone),
        error = errorFor(GuestError.InvalidPhone)?.let { stringResource(it.message()) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        FieldLabel(text = stringResource(Res.string.guest_form_document_type))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            DocumentType.entries.forEach { type ->
                MajesticChip(
                    text = stringResource(type.label()),
                    selected = form.documentType == type,
                    onClick = { onIntent(NewReservationIntent.DocumentTypeSelect(type)) },
                )
            }
        }
    }
    MajesticTextField(
        value = form.documentNumber,
        onValueChange = { onIntent(NewReservationIntent.DocumentNumberChange(it)) },
        label = stringResource(Res.string.guest_form_document_number),
        help = stringResource(Res.string.guest_form_document_hint),
        error = errorFor(GuestError.IncompleteDocument, GuestError.DuplicateDocument)?.let { stringResource(it.message()) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    MajesticTextField(
        value = form.nationality,
        onValueChange = { onIntent(NewReservationIntent.NationalityChange(it)) },
        label = stringResource(Res.string.guest_form_nationality),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    MajesticTextField(
        value = form.email,
        onValueChange = { onIntent(NewReservationIntent.EmailChange(it)) },
        label = stringResource(Res.string.guest_form_email),
        error = errorFor(GuestError.InvalidEmail)?.let { stringResource(it.message()) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End)) {
        MajesticButton(text = stringResource(Res.string.cancel), onClick = { onIntent(NewReservationIntent.DismissGuestForm) }, style = ButtonStyle.Text)
        MajesticButton(text = stringResource(Res.string.save), onClick = { onIntent(NewReservationIntent.SaveGuest) }, enabled = !isSaving, loading = isSaving)
    }
}

private const val MAX_NOTES_LINES = 8
