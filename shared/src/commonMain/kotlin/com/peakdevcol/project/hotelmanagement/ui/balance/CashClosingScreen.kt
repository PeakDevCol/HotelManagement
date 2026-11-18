package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosing
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingDraft
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingError
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_cash_sales
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_cash_tips
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_confirm_message
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_confirm_title
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_counted
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_difference_none
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_difference_over
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_difference_short
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_already_closed
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_count
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_no_connection
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_note
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_note_long
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_type
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_expected
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_note
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_note_required
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_number
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_payment_count
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_recent
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_recent_amounts
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_recent_empty
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_recent_line
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_save
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_saved
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_since_previous
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_since_start
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_title
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_transfers
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_type
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_type_daily
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_type_hint
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_type_manual
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue
import kotlin.time.Instant

/** RF-BAL-06: only administrators open it, from the balance. */
@Composable
fun CashClosingScreen(onBack: () -> Unit, viewModel: CashClosingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> snackbarHostState.showSnackbar(effect.message()) }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.cash_closing_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val overview = state.overview
        val loadError = state.loadError
        when {
            overview == null && loadError != null -> CenteredMessage(
                message = stringResource(loadError.messageRes()),
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.onIntent(CashClosingIntent.Reload) },
            )
            overview == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    loadError?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
                    ExpectedCash(overview.draft)
                    ClosingInput(state = state, onIntent = viewModel::onIntent)
                    HorizontalDivider()
                    RecentClosings(closings = overview.recentClosings, employeeNames = state.employeeNames)
                }
            }
        }
    }

    val draft = state.overview?.draft
    val counted = state.countedCash
    if (state.isConfirming && draft != null && counted != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(CashClosingIntent.DismissConfirmation) },
            title = { Text(stringResource(Res.string.cash_closing_confirm_title)) },
            text = { Text(stringResource(Res.string.cash_closing_confirm_message, formatPesos(draft.expectedCash), formatPesos(counted))) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(CashClosingIntent.ConfirmSave) }) {
                    Text(stringResource(Res.string.cash_closing_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(CashClosingIntent.DismissConfirmation) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.isSaving || (state.isLoading && state.overview != null)) LoadingOverlay()
}

@Composable
private fun ExpectedCash(draft: CashClosingDraft) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.cash_closing_number, draft.number), style = MaterialTheme.typography.titleMedium)
            val lastClosing = draft.lastClosing
            Hint(
                if (lastClosing == null) {
                    stringResource(Res.string.cash_closing_since_start)
                } else {
                    stringResource(Res.string.cash_closing_since_previous, lastClosing.number, lastClosing.createdAt.formatted())
                },
            )
            Hint(stringResource(Res.string.cash_closing_payment_count, draft.payments.size))
            HorizontalDivider()
            AmountLine(stringResource(Res.string.cash_closing_cash_sales), draft.cashSales)
            AmountLine(stringResource(Res.string.cash_closing_cash_tips), draft.cashTips)
            AmountLine(stringResource(Res.string.cash_closing_expected), draft.expectedCash, emphasized = true)
            AmountLine(stringResource(Res.string.cash_closing_transfers), draft.transferTotal)
        }
    }
}

@Composable
private fun ClosingInput(state: CashClosingState, onIntent: (CashClosingIntent) -> Unit) {
    val error = state.inputError
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.cash_closing_type), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashClosingType.entries.forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { onIntent(CashClosingIntent.SelectType(type)) },
                    label = { Text(stringResource(type.label())) },
                )
            }
        }
        Hint(stringResource(Res.string.cash_closing_type_hint))
        if (error == CashClosingError.MissingType) ErrorText(stringResource(Res.string.cash_closing_error_type))

        OutlinedTextField(
            value = state.countedText,
            onValueChange = { onIntent(CashClosingIntent.CountedChange(it)) },
            label = { Text(stringResource(Res.string.cash_closing_counted)) },
            prefix = { Text("$") },
            isError = error == CashClosingError.InvalidCount,
            supportingText = if (error == CashClosingError.InvalidCount) {
                { Text(stringResource(Res.string.cash_closing_error_count)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        state.difference?.let { difference -> DifferenceText(difference) }

        val noteRequired = state.difference?.let { it != 0L } == true
        OutlinedTextField(
            value = state.note,
            onValueChange = { onIntent(CashClosingIntent.NoteChange(it)) },
            label = { Text(stringResource(if (noteRequired) Res.string.cash_closing_note_required else Res.string.cash_closing_note)) },
            isError = error == CashClosingError.MissingNote || error == CashClosingError.NoteTooLong,
            supportingText = when (error) {
                CashClosingError.MissingNote -> {
                    { Text(stringResource(Res.string.cash_closing_error_note)) }
                }
                CashClosingError.NoteTooLong -> {
                    { Text(stringResource(Res.string.cash_closing_error_note_long)) }
                }
                else -> null
            },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(Res.string.cash_closing_save),
            onClick = { onIntent(CashClosingIntent.RequestSave) },
            enabled = !state.isSaving && !state.isLoading,
        )
    }
}

@Composable
private fun DifferenceText(difference: Long) {
    val text = when {
        difference == 0L -> stringResource(Res.string.cash_closing_difference_none)
        difference > 0 -> stringResource(Res.string.cash_closing_difference_over, formatPesos(difference))
        else -> stringResource(Res.string.cash_closing_difference_short, formatPesos(difference.absoluteValue))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = if (difference == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun RecentClosings(closings: List<CashClosing>, employeeNames: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.cash_closing_recent), style = MaterialTheme.typography.titleMedium)
        if (closings.isEmpty()) Hint(stringResource(Res.string.cash_closing_recent_empty))
        closings.forEach { closing ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(
                        Res.string.cash_closing_recent_line,
                        stringResource(Res.string.cash_closing_number, closing.number),
                        stringResource(closing.type.label()),
                        closing.createdAt.formatted(),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Hint(
                    stringResource(
                        Res.string.cash_closing_recent_amounts,
                        formatPesos(closing.expectedCash),
                        formatPesos(closing.countedCash),
                        formatPesos(closing.difference),
                    ),
                )
                Hint(employeeNameOrUnknown(employeeNames[closing.closedBy]))
                if (closing.note.isNotEmpty()) Hint(closing.note)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun AmountLine(title: String, amount: Long, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = style, modifier = Modifier.weight(1f))
        Text(formatPesos(amount), style = style)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

private fun Instant?.formatted(): String = this?.let(::formatBogotaDateTime) ?: "—"

fun CashClosingType.label(): StringResource = when (this) {
    CashClosingType.DAILY -> Res.string.cash_closing_type_daily
    CashClosingType.MANUAL -> Res.string.cash_closing_type_manual
}

private fun CashClosingError.messageRes(): StringResource = when (this) {
    CashClosingError.MissingType -> Res.string.cash_closing_error_type
    CashClosingError.InvalidCount -> Res.string.cash_closing_error_count
    CashClosingError.MissingNote -> Res.string.cash_closing_error_note
    CashClosingError.NoteTooLong -> Res.string.cash_closing_error_note_long
    CashClosingError.NoSession -> Res.string.order_error_no_session
    CashClosingError.NotAllowed -> Res.string.cash_closing_error_not_allowed
    CashClosingError.AlreadyClosed -> Res.string.cash_closing_error_already_closed
    CashClosingError.NoConnection -> Res.string.cash_closing_error_no_connection
    CashClosingError.Unknown -> Res.string.cash_closing_error_unknown
}

private suspend fun CashClosingEffect.message(): String = when (this) {
    CashClosingEffect.Saved -> getString(Res.string.cash_closing_saved)
    is CashClosingEffect.SaveFailed -> getString(error.messageRes())
}
