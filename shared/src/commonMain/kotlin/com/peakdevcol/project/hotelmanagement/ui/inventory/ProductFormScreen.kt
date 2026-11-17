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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.inventory_edit_product
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_blank_name
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_missing_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_price
import com.peakdevcol.project.hotelmanagement.resources.inventory_new_product
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_active
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_active_hint
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_name
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_not_found
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_price
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProductFormScreen(
    productId: String?,
    onBack: () -> Unit,
    viewModel: ProductFormViewModel = koinViewModel(key = productId ?: NEW_PRODUCT_KEY) { parametersOf(productId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductFormEffect.Saved -> onBack()
                is ProductFormEffect.SaveFailed -> snackbarHostState.showSnackbar(getString(effect.error.message()))
            }
        }
    }

    val title = if (state.isNew) Res.string.inventory_new_product else Res.string.inventory_edit_product
    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.notFound -> CenteredMessage(
                message = stringResource(Res.string.inventory_product_not_found),
                modifier = Modifier.padding(padding),
            )
            else -> ProductForm(state = state, onIntent = viewModel::onIntent, modifier = Modifier.padding(padding))
        }
    }

    if (state.isSaving) LoadingOverlay()
}

@Composable
private fun ProductForm(state: ProductFormState, onIntent: (ProductFormIntent) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onIntent(ProductFormIntent.NameChange(it)) },
                label = { Text(stringResource(Res.string.inventory_product_name)) },
                isError = state.fieldError == InventoryError.BlankName,
                supportingText = errorText(state.fieldError, InventoryError.BlankName),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.priceText,
                onValueChange = { onIntent(ProductFormIntent.PriceChange(it)) },
                label = { Text(stringResource(Res.string.inventory_product_price)) },
                prefix = { Text("$") },
                isError = state.fieldError == InventoryError.InvalidPrice,
                supportingText = errorText(state.fieldError, InventoryError.InvalidPrice),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            CategoryPicker(state = state, onSelect = { onIntent(ProductFormIntent.CategorySelect(it)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.inventory_product_active), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(Res.string.inventory_product_active_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.active, onCheckedChange = { onIntent(ProductFormIntent.ActiveChange(it)) })
            }
            PrimaryButton(
                text = stringResource(Res.string.save),
                onClick = { onIntent(ProductFormIntent.Save) },
                enabled = state.canSave,
            )
        }
    }
}

@Composable
private fun CategoryPicker(state: ProductFormState, onSelect: (String) -> Unit) {
    val missing = state.fieldError == InventoryError.MissingCategory
    Column(modifier = Modifier.selectableGroup()) {
        Text(
            text = stringResource(Res.string.inventory_product_category),
            style = MaterialTheme.typography.titleSmall,
            color = if (missing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        state.categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = state.categoryId == category.id, onClick = { onSelect(category.id) }, role = Role.RadioButton)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.categoryId == category.id, onClick = null)
                Text(category.name, modifier = Modifier.padding(start = 12.dp))
            }
        }
        if (missing) {
            Text(
                text = stringResource(Res.string.inventory_error_missing_category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun errorText(current: InventoryError?, expected: InventoryError): (@Composable () -> Unit)? =
    if (current == expected) ({ Text(stringResource(expected.message())) }) else null

private const val NEW_PRODUCT_KEY = "new-product"
