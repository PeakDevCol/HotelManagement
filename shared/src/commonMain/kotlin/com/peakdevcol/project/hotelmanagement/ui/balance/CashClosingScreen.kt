package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_count_title
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
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_error_title
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
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_recent_meta
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
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.currentorders.employeeNameOrUnknown
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ConfirmDialog
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MoneyField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SegmentedControl
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue
import kotlin.time.Instant

/** RF-BAL-06: only administrators open it, from the balance. */
@Composable
fun CashClosingScreen(onBack: () -> Unit, viewModel: CashClosingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }

    CashClosingContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun CashClosingContent(state: CashClosingState, onIntent: (CashClosingIntent) -> Unit, onBack: () -> Unit) {
    val overview = state.overview
    val loadError = state.loadError
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.cash_closing_title), onBack = onBack) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            overview == null && loadError != null -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.cash_closing_error_title),
                message = stringResource(loadError.messageRes()),
                actionText = stringResource(Res.string.retry),
                onAction = { onIntent(CashClosingIntent.Reload) },
                modifier = Modifier.padding(padding),
            )
            overview == null -> MajesticLoading(Modifier.padding(padding))
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MajesticSizes.content)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(MajesticSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
                ) {
                    loadError?.let { Banner(tone = BannerTone.Danger, message = stringResource(it.messageRes())) }
                    ExpectedCash(overview.draft)
                    ClosingInput(state = state, onIntent = onIntent)
                    RecentClosings(closings = overview.recentClosings, employeeNames = state.employeeNames)
                }
            }
        }
    }

    val draft = state.overview?.draft
    val counted = state.countedCash
    if (state.isConfirming && draft != null && counted != null) {
        ConfirmDialog(
            title = stringResource(Res.string.cash_closing_confirm_title),
            message = stringResource(Res.string.cash_closing_confirm_message, formatPesos(draft.expectedCash), formatPesos(counted)),
            confirmText = stringResource(Res.string.cash_closing_save),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onIntent(CashClosingIntent.ConfirmSave) },
            onDismiss = { onIntent(CashClosingIntent.DismissConfirmation) },
        )
    }
}

/** What must be in the drawer since the last closing, and the transfers, which are not in it. */
@Composable
private fun ExpectedCash(draft: CashClosingDraft) {
    val lastClosing = draft.lastClosing
    SectionCard(
        title = stringResource(Res.string.cash_closing_number, draft.number),
        help = listOf(
            if (lastClosing == null) {
                stringResource(Res.string.cash_closing_since_start)
            } else {
                stringResource(Res.string.cash_closing_since_previous, lastClosing.number, lastClosing.createdAt.formatted())
            },
            pluralStringResource(Res.plurals.cash_closing_payment_count, draft.payments.size, draft.payments.size),
        ).joinToString("\n"),
    ) {
        AmountRow(label = stringResource(Res.string.cash_closing_cash_sales), amount = draft.cashSales)
        AmountRow(label = stringResource(Res.string.cash_closing_cash_tips), amount = draft.cashTips)
        ListDivider()
        AmountRow(label = stringResource(Res.string.cash_closing_expected), amount = draft.expectedCash, emphasized = true)
        AmountRow(label = stringResource(Res.string.cash_closing_transfers), amount = draft.transferTotal)
    }
}

@Composable
private fun ClosingInput(state: CashClosingState, onIntent: (CashClosingIntent) -> Unit) {
    val error = state.inputError
    SectionCard(title = stringResource(Res.string.cash_closing_count_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            FieldLabel(text = stringResource(Res.string.cash_closing_type))
            // No type comes marked: the choice decides whether the day counts as closed.
            SegmentedControl(
                options = CashClosingType.entries,
                selected = state.type,
                onSelect = { onIntent(CashClosingIntent.SelectType(it)) },
                label = { stringResource(it.label()) },
            )
            Text(
                text = stringResource(if (error == CashClosingError.MissingType) Res.string.cash_closing_error_type else Res.string.cash_closing_type_hint),
                style = MajesticTheme.typography.caption,
                color = if (error == CashClosingError.MissingType) MajesticTheme.colors.danger else MajesticTheme.colors.inkMuted,
            )
        }
        MoneyField(
            digits = state.countedText,
            onDigitsChange = { onIntent(CashClosingIntent.CountedChange(it)) },
            label = stringResource(Res.string.cash_closing_counted),
            error = if (error == CashClosingError.InvalidCount) stringResource(Res.string.cash_closing_error_count) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        state.difference?.let { DifferenceBanner(it) }
        val noteRequired = state.difference?.let { it != 0L } == true
        MajesticTextField(
            value = state.note,
            onValueChange = { onIntent(CashClosingIntent.NoteChange(it)) },
            label = stringResource(if (noteRequired) Res.string.cash_closing_note_required else Res.string.cash_closing_note),
            error = when (error) {
                CashClosingError.MissingNote -> stringResource(Res.string.cash_closing_error_note)
                CashClosingError.NoteTooLong -> stringResource(Res.string.cash_closing_error_note_long)
                else -> null
            },
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        MajesticButton(
            text = stringResource(Res.string.cash_closing_save),
            onClick = { onIntent(CashClosingIntent.RequestSave) },
            size = ButtonSize.Large,
            enabled = !state.isLoading,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** RF-BAL-06: with a difference the note becomes required. Said in words, never only with a color. */
@Composable
private fun DifferenceBanner(difference: Long) {
    when {
        difference == 0L -> Banner(tone = BannerTone.Info, icon = MajesticIcons.checkCircle, message = stringResource(Res.string.cash_closing_difference_none))
        difference > 0 -> Banner(tone = BannerTone.Warning, icon = MajesticIcons.warning, message = stringResource(Res.string.cash_closing_difference_over, formatPesos(difference)))
        else -> Banner(tone = BannerTone.Danger, icon = MajesticIcons.error, message = stringResource(Res.string.cash_closing_difference_short, formatPesos(difference.absoluteValue)))
    }
}

@Composable
private fun RecentClosings(closings: List<CashClosing>, employeeNames: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        SectionLabel(text = stringResource(Res.string.cash_closing_recent))
        if (closings.isEmpty()) {
            Text(text = stringResource(Res.string.cash_closing_recent_empty), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            return@Column
        }
        ListGroup {
            closings.forEachIndexed { index, closing ->
                if (index > 0) ListDivider()
                ClosingRow(closing = closing, employeeName = employeeNameOrUnknown(employeeNames[closing.closedBy]))
            }
        }
    }
}

@Composable
private fun ClosingRow(closing: CashClosing, employeeName: String) {
    val typography = MajesticTheme.typography
    val colors = MajesticTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(MajesticSpacing.space4), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space1)) {
        Text(
            text = listOf(stringResource(Res.string.cash_closing_number, closing.number), stringResource(closing.type.label())).joinToString(" · "),
            style = typography.bodyLarge,
            color = colors.ink,
        )
        Text(
            text = stringResource(Res.string.cash_closing_recent_amounts, formatPesos(closing.expectedCash), formatPesos(closing.countedCash), formatPesos(closing.difference)),
            style = typography.body,
            color = colors.inkMuted,
        )
        Text(text = stringResource(Res.string.cash_closing_recent_meta, closing.createdAt.formatted(), employeeName), style = typography.caption, color = colors.inkMuted)
        if (closing.note.isNotEmpty()) Text(text = closing.note, style = typography.caption, color = colors.inkMuted)
    }
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
