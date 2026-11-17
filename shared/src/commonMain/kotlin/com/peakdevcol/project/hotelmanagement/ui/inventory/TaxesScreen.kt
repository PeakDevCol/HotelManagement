package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.formatTaxRate
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.ic_add
import com.peakdevcol.project.hotelmanagement.resources.inventory_error
import com.peakdevcol.project.hotelmanagement.resources.inventory_taxes
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.taxes_active
import com.peakdevcol.project.hotelmanagement.resources.taxes_active_hint
import com.peakdevcol.project.hotelmanagement.resources.taxes_edit
import com.peakdevcol.project.hotelmanagement.resources.taxes_empty
import com.peakdevcol.project.hotelmanagement.resources.taxes_inactive
import com.peakdevcol.project.hotelmanagement.resources.taxes_name
import com.peakdevcol.project.hotelmanagement.resources.taxes_new
import com.peakdevcol.project.hotelmanagement.resources.taxes_rate
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TaxesScreen(onBack: () -> Unit, viewModel: TaxesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TaxesEffect.Failed -> snackbarHostState.showSnackbar(getString(effect.error.message()))
            }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.inventory_taxes), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onIntent(TaxesIntent.AddClick) }) {
                Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.taxes_new))
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed -> CenteredMessage(stringResource(Res.string.inventory_error), Modifier.padding(padding))
            state.taxes.isEmpty() -> CenteredMessage(stringResource(Res.string.taxes_empty), Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                items(state.taxes, key = { it.id }) { tax ->
                    TaxRow(tax = tax, onClick = { viewModel.onIntent(TaxesIntent.EditClick(tax)) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    state.form?.let { form -> TaxFormDialog(form = form, enabled = !state.isSaving, onIntent = viewModel::onIntent) }
}

@Composable
private fun TaxRow(tax: Tax, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tax.name, style = MaterialTheme.typography.bodyLarge)
            if (!tax.active) {
                Text(
                    text = stringResource(Res.string.taxes_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(text = formatTaxRate(tax.rate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TaxFormDialog(form: TaxForm, enabled: Boolean, onIntent: (TaxesIntent) -> Unit) {
    val title = if (form.taxId == null) stringResource(Res.string.taxes_new) else stringResource(Res.string.taxes_edit, form.name)
    AlertDialog(
        onDismissRequest = { onIntent(TaxesIntent.DismissForm) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onIntent(TaxesIntent.NameChange(it)) },
                    label = { Text(stringResource(Res.string.taxes_name)) },
                    isError = form.error == InventoryError.BlankName,
                    supportingText = fieldError(form.error, InventoryError.BlankName),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.rateText,
                    onValueChange = { onIntent(TaxesIntent.RateChange(it)) },
                    label = { Text(stringResource(Res.string.taxes_rate)) },
                    suffix = { Text("%") },
                    isError = form.error == InventoryError.InvalidTaxRate,
                    supportingText = fieldError(form.error, InventoryError.InvalidTaxRate),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.taxes_active), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(Res.string.taxes_active_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = form.active, onCheckedChange = { onIntent(TaxesIntent.ActiveChange(it)) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onIntent(TaxesIntent.Save) }, enabled = enabled) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(TaxesIntent.DismissForm) }) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

private fun fieldError(current: InventoryError?, expected: InventoryError): (@Composable () -> Unit)? =
    if (current == expected) ({ Text(stringResource(expected.message())) }) else null
