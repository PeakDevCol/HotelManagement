package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.inventory_back_to_list
import com.peakdevcol.project.hotelmanagement.resources.inventory_edit_product
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_missing_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_new_product
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_active
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_active_hint
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_name
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_not_found
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_price
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_saved
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_tax
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_tax_hint
import com.peakdevcol.project.hotelmanagement.resources.inventory_tax_none
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSwitchRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MoneyField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** RF-INV-02, RF-INV-03 and RF-INV-07. A null [productId] creates a product. */
@Composable
fun ProductFormScreen(
    productId: String?,
    onBack: () -> Unit,
    viewModel: ProductFormViewModel = koinViewModel(key = productId ?: NEW_PRODUCT_KEY) { parametersOf(productId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductFormEffect.Saved -> {
                    messages.show(getString(Res.string.inventory_product_saved))
                    onBack()
                }
                is ProductFormEffect.SaveFailed -> messages.show(getString(effect.error.message()))
            }
        }
    }

    ProductFormContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun ProductFormContent(state: ProductFormState, onIntent: (ProductFormIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            MajesticTopBar(title = stringResource(if (state.isNew) Res.string.inventory_new_product else Res.string.inventory_edit_product), onBack = onBack)
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.notFound -> EmptyState(
                icon = MajesticIcons.inventory,
                title = stringResource(Res.string.inventory_product_not_found),
                message = stringResource(Res.string.inventory_back_to_list),
                modifier = Modifier.padding(padding),
            )
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                ProductForm(state = state, onIntent = onIntent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductForm(state: ProductFormState, onIntent: (ProductFormIntent) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = MajesticSizes.content)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(MajesticSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space5),
    ) {
        MajesticTextField(
            value = state.name,
            onValueChange = { onIntent(ProductFormIntent.NameChange(it)) },
            label = stringResource(Res.string.inventory_product_name),
            error = state.fieldError.takeIf { it == InventoryError.BlankName }?.let { stringResource(it.message()) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        MoneyField(
            digits = state.priceText,
            onDigitsChange = { onIntent(ProductFormIntent.PriceChange(it)) },
            label = stringResource(Res.string.inventory_product_price),
            error = state.fieldError.takeIf { it == InventoryError.InvalidPrice }?.let { stringResource(it.message()) },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            FieldLabel(text = stringResource(Res.string.inventory_product_category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                state.categories.forEach { category ->
                    MajesticChip(
                        text = category.name,
                        selected = state.categoryId == category.id,
                        onClick = { onIntent(ProductFormIntent.CategorySelect(category.id)) },
                    )
                }
            }
            if (state.fieldError == InventoryError.MissingCategory) {
                Text(text = stringResource(Res.string.inventory_error_missing_category), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.danger)
            }
        }
        if (state.taxes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                FieldLabel(text = stringResource(Res.string.inventory_product_tax))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                    MajesticChip(text = stringResource(Res.string.inventory_tax_none), selected = state.taxId == null, onClick = { onIntent(ProductFormIntent.TaxSelect(null)) })
                    state.taxes.forEach { tax ->
                        MajesticChip(text = tax.label(), selected = state.taxId == tax.id, onClick = { onIntent(ProductFormIntent.TaxSelect(tax.id)) })
                    }
                }
                Text(text = stringResource(Res.string.inventory_product_tax_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
            }
        }
        MajesticSwitchRow(
            title = stringResource(Res.string.inventory_product_active),
            help = stringResource(Res.string.inventory_product_active_hint),
            checked = state.active,
            onCheckedChange = { onIntent(ProductFormIntent.ActiveChange(it)) },
        )
        MajesticButton(
            text = stringResource(Res.string.save),
            onClick = { onIntent(ProductFormIntent.Save) },
            size = ButtonSize.Large,
            enabled = state.canSave,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val NEW_PRODUCT_KEY = "new-product"
