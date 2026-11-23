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
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReservationDetailSheet(reservation: Reservation, onIntent: (ReservationsIntent) -> Unit) {
    TaskSheet(
        title = reservation.guestName,
        subtitle = stringResource(Res.string.destination_room_number, reservation.roomNumber),
        onDismiss = { onIntent(ReservationsIntent.CloseReservation) },
        asDialog = LocalWideWindow.current,
    ) {
        ReservationDetailFields(reservation)
    }
}

/** RF-RES-02: the days, the people, what the stay costs and how it reached the hotel. Changing it comes in the second PR of M11. */
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
