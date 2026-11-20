package com.peakdevcol.project.hotelmanagement.ui.inventory

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.formatTaxRate
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.inventory_error
import com.peakdevcol.project.hotelmanagement.resources.inventory_taxes
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.taxes_active
import com.peakdevcol.project.hotelmanagement.resources.taxes_active_hint
import com.peakdevcol.project.hotelmanagement.resources.taxes_edit
import com.peakdevcol.project.hotelmanagement.resources.taxes_empty
import com.peakdevcol.project.hotelmanagement.resources.taxes_empty_title
import com.peakdevcol.project.hotelmanagement.resources.taxes_inactive
import com.peakdevcol.project.hotelmanagement.resources.taxes_name
import com.peakdevcol.project.hotelmanagement.resources.taxes_new
import com.peakdevcol.project.hotelmanagement.resources.taxes_rate
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSwitchRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-INV-07: the taxes the prices can include. */
@Composable
fun TaxesScreen(onBack: () -> Unit, viewModel: TaxesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TaxesEffect.Failed -> messages.show(getString(effect.error.message()))
            }
        }
    }

    TaxesContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun TaxesContent(state: TaxesState, onIntent: (TaxesIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.inventory_taxes), onBack = onBack) },
        floatingActionButton = {
            MajesticFloatingActionButton(text = stringResource(Res.string.taxes_new), icon = MajesticIcons.add, onClick = { onIntent(TaxesIntent.AddClick) })
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.loadFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.inventory_error),
                message = stringResource(Res.string.error_check_connection),
                modifier = Modifier.padding(padding),
            )
            state.taxes.isEmpty() -> EmptyState(
                icon = MajesticIcons.percent,
                title = stringResource(Res.string.taxes_empty_title),
                message = stringResource(Res.string.taxes_empty),
                modifier = Modifier.padding(padding),
            )
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MajesticSizes.content)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = MajesticSpacing.space4, end = MajesticSpacing.space4, top = MajesticSpacing.space2, bottom = MajesticSizes.floatingActionButtonClearance),
                ) {
                    ListGroup {
                        state.taxes.forEachIndexed { index, tax ->
                            if (index > 0) ListDivider()
                            TaxRow(tax = tax, onClick = { onIntent(TaxesIntent.EditClick(tax)) })
                        }
                    }
                }
            }
        }
    }
    state.form?.let { form -> TaxFormSheet(form = form, isSaving = state.isSaving, onIntent = onIntent) }
}

@Composable
private fun TaxRow(tax: Tax, onClick: () -> Unit) {
    ListRow(
        title = tax.name,
        subtitle = if (tax.active) null else stringResource(Res.string.taxes_inactive),
        trailing = { Text(text = formatTaxRate(tax.rate), style = MajesticTheme.typography.label, color = MajesticTheme.colors.ink, maxLines = 1) },
        onClick = onClick,
    )
}

@Composable
private fun TaxFormSheet(form: TaxForm, isSaving: Boolean, onIntent: (TaxesIntent) -> Unit) {
    val dismiss = { onIntent(TaxesIntent.DismissForm) }
    TaskSheet(
        title = if (form.taxId == null) stringResource(Res.string.taxes_new) else stringResource(Res.string.taxes_edit, form.name),
        onDismiss = dismiss,
        asDialog = LocalWideWindow.current,
    ) {
        MajesticTextField(
            value = form.name,
            onValueChange = { onIntent(TaxesIntent.NameChange(it)) },
            label = stringResource(Res.string.taxes_name),
            error = form.error.takeIf { it == InventoryError.BlankName }?.let { stringResource(it.message()) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        MajesticTextField(
            value = form.rateText,
            onValueChange = { onIntent(TaxesIntent.RateChange(it)) },
            label = stringResource(Res.string.taxes_rate),
            error = form.error.takeIf { it == InventoryError.InvalidTaxRate }?.let { stringResource(it.message()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            trailing = {
                Text(text = "%", style = MajesticTheme.typography.bodyLarge, color = MajesticTheme.colors.inkMuted, modifier = Modifier.padding(end = MajesticSpacing.space3))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        MajesticSwitchRow(
            title = stringResource(Res.string.taxes_active),
            help = stringResource(Res.string.taxes_active_hint),
            checked = form.active,
            onCheckedChange = { onIntent(TaxesIntent.ActiveChange(it)) },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End)) {
            MajesticButton(text = stringResource(Res.string.cancel), onClick = dismiss, style = ButtonStyle.Text)
            MajesticButton(text = stringResource(Res.string.save), onClick = { onIntent(TaxesIntent.Save) }, enabled = !isSaving, loading = isSaving)
        }
    }
}
