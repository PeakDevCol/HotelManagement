package com.peakdevcol.project.hotelmanagement.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceError
import com.peakdevcol.project.hotelmanagement.domain.model.BalanceReport
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentReadError
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.formatTimeOfDay
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.balance_change_period
import com.peakdevcol.project.hotelmanagement.resources.balance_clear_filters
import com.peakdevcol.project.hotelmanagement.resources.balance_days_without_closing_title
import com.peakdevcol.project.hotelmanagement.resources.balance_error_no_connection
import com.peakdevcol.project.hotelmanagement.resources.balance_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.balance_error_title
import com.peakdevcol.project.hotelmanagement.resources.balance_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.balance_filters
import com.peakdevcol.project.hotelmanagement.resources.balance_filters_active
import com.peakdevcol.project.hotelmanagement.resources.balance_period
import com.peakdevcol.project.hotelmanagement.resources.balance_period_hint
import com.peakdevcol.project.hotelmanagement.resources.balance_period_invalid
import com.peakdevcol.project.hotelmanagement.resources.balance_period_too_long
import com.peakdevcol.project.hotelmanagement.resources.balance_updating
import com.peakdevcol.project.hotelmanagement.resources.cash_closing_open
import com.peakdevcol.project.hotelmanagement.resources.nav_cash_and_balance
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.DaysRangeDialog
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-BAL-01 to RF-BAL-04 and RF-BAL-07: only administrators open it (PRD, section 3). */
@Composable
fun BalanceScreen(onCashClosingClick: () -> Unit, onBack: (() -> Unit)? = null, viewModel: BalanceViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }

    BalanceContent(state = state, onIntent = viewModel::onIntent, onCashClosingClick = onCashClosingClick, onBack = onBack)
}

/** [onBack] is the arrow back to "Más", when the section was opened from there on the phone. */
@Composable
fun BalanceContent(state: BalanceState, onIntent: (BalanceIntent) -> Unit, onCashClosingClick: () -> Unit, onBack: (() -> Unit)? = null) {
    val report = state.report
    val loadError = state.loadError
    Scaffold(
        topBar = {
            MajesticTopBar(title = stringResource(Res.string.nav_cash_and_balance), onBack = onBack) {
                MajesticIconButton(
                    icon = MajesticIcons.calendar,
                    contentDescription = stringResource(Res.string.balance_change_period),
                    onClick = { onIntent(BalanceIntent.OpenPeriodPicker) },
                    tint = MajesticTheme.colors.ink,
                    enabled = report != null && !state.isLoading,
                )
                MajesticIconButton(
                    icon = MajesticIcons.filter,
                    contentDescription = stringResource(Res.string.balance_filters),
                    onClick = { onIntent(BalanceIntent.OpenFilters) },
                    tint = MajesticTheme.colors.ink,
                    enabled = report != null,
                )
            }
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            report == null && loadError != null -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.balance_error_title),
                message = stringResource(loadError.messageRes()),
                actionText = stringResource(Res.string.retry),
                onAction = { onIntent(BalanceIntent.Reload) },
                modifier = Modifier.padding(padding),
            )
            report == null -> MajesticLoading(Modifier.padding(padding))
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(MajesticSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
                ) {
                    PeriodHeader(report = report, isUpdating = state.isLoading)
                    BalanceNotices(state = state, onIntent = onIntent)
                    BalanceTiles(state = state)
                    MajesticButton(
                        text = stringResource(Res.string.cash_closing_open),
                        onClick = onCashClosingClick,
                        icon = MajesticIcons.pointOfSale,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.summary?.let { summary ->
                        Breakdowns(
                            summary = summary,
                            selected = state.breakdown,
                            daysWithoutDailyClosing = state.daysWithoutDailyClosing,
                            onSelect = { onIntent(BalanceIntent.ShowBreakdown(it)) },
                        )
                    }
                }
            }
        }
    }

    val period = state.period
    if (state.isPickingPeriod && period != null) {
        DaysRangeDialog(
            firstDay = period.firstDay,
            lastDay = period.lastDay,
            onConfirm = { first, last -> onIntent(BalanceIntent.ChangePeriod(first, last)) },
            onDismiss = { onIntent(BalanceIntent.DismissPeriodPicker) },
        )
    }
    if (state.isChoosingFilters) BalanceFiltersSheet(state = state, onIntent = onIntent)
}

@Composable
private fun PeriodHeader(report: BalanceReport, isUpdating: Boolean) {
    Column {
        Text(
            text = stringResource(Res.string.balance_period, formatCalendarDate(report.period.firstDay), formatCalendarDate(report.period.lastDay)),
            style = MajesticTheme.typography.title,
            color = MajesticTheme.colors.ink,
        )
        Text(
            text = if (isUpdating) stringResource(Res.string.balance_updating) else stringResource(Res.string.balance_period_hint, formatTimeOfDay(report.dayStartMinutes)),
            style = MajesticTheme.typography.caption,
            color = MajesticTheme.colors.inkMuted,
        )
    }
}

/** A reading that failed after a good one, the days without closing (RF-BAL-06) and the filters in use. */
@Composable
private fun BalanceNotices(state: BalanceState, onIntent: (BalanceIntent) -> Unit) {
    state.loadError?.let { error ->
        Banner(tone = BannerTone.Danger, message = stringResource(error.messageRes()), actionText = stringResource(Res.string.retry), onAction = { onIntent(BalanceIntent.Reload) })
    }
    val daysWithoutClosing = state.daysWithoutDailyClosing
    if (daysWithoutClosing.isNotEmpty()) {
        Banner(
            tone = BannerTone.Warning,
            icon = MajesticIcons.warning,
            title = pluralStringResource(Res.plurals.balance_days_without_closing_title, daysWithoutClosing.size, daysWithoutClosing.size),
            message = daysWithoutClosing.sortedBy { it.epochDay }.joinToString(", ") { formatCalendarDate(it) },
        )
    }
    if (state.activeFilterCount > 0) {
        Banner(
            tone = BannerTone.Info,
            icon = MajesticIcons.filter,
            message = pluralStringResource(Res.plurals.balance_filters_active, state.activeFilterCount, state.activeFilterCount),
            actionText = stringResource(Res.string.balance_clear_filters),
            onAction = { onIntent(BalanceIntent.ClearFilters) },
        )
    }
}

fun PaymentReadError.messageRes(): StringResource = when (this) {
    PaymentReadError.NoConnection -> Res.string.balance_error_no_connection
    PaymentReadError.NotAllowed -> Res.string.balance_error_not_allowed
    PaymentReadError.Unknown -> Res.string.balance_error_unknown
}

private suspend fun BalanceEffect.message(): String = when (this) {
    is BalanceEffect.PeriodRejected -> getString(
        when (error) {
            BalanceError.InvalidPeriod -> Res.string.balance_period_invalid
            BalanceError.PeriodTooLong -> Res.string.balance_period_too_long
        },
    )
}
