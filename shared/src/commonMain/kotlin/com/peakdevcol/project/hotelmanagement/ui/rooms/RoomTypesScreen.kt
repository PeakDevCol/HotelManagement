package com.peakdevcol.project.hotelmanagement.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.peakdevcol.project.hotelmanagement.domain.model.RoomError
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_tax
import com.peakdevcol.project.hotelmanagement.resources.inventory_tax_none
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.resources.room_types_active
import com.peakdevcol.project.hotelmanagement.resources.room_types_active_hint
import com.peakdevcol.project.hotelmanagement.resources.room_types_capacity
import com.peakdevcol.project.hotelmanagement.resources.room_types_capacity_hint
import com.peakdevcol.project.hotelmanagement.resources.room_types_edit
import com.peakdevcol.project.hotelmanagement.resources.room_types_empty
import com.peakdevcol.project.hotelmanagement.resources.room_types_empty_title
import com.peakdevcol.project.hotelmanagement.resources.room_types_error
import com.peakdevcol.project.hotelmanagement.resources.room_types_name
import com.peakdevcol.project.hotelmanagement.resources.room_types_name_hint
import com.peakdevcol.project.hotelmanagement.resources.room_types_new
import com.peakdevcol.project.hotelmanagement.resources.room_types_rate
import com.peakdevcol.project.hotelmanagement.resources.room_types_rate_hint
import com.peakdevcol.project.hotelmanagement.resources.rooms_types
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.taxes_inactive
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountText
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSwitchRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MoneyField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.inventory.label
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-HAB-01: the types of room, opened from the board by an administrator (decision 1A). */
@Composable
fun RoomTypesScreen(onBack: () -> Unit, viewModel: RoomTypesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RoomTypesEffect.Failed -> messages.show(getString(effect.error.message()))
            }
        }
    }

    RoomTypesContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun RoomTypesContent(state: RoomTypesState, onIntent: (RoomTypesIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.rooms_types), onBack = onBack) },
        floatingActionButton = {
            MajesticFloatingActionButton(text = stringResource(Res.string.room_types_new), icon = MajesticIcons.add, onClick = { onIntent(RoomTypesIntent.AddClick) })
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.loadFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.room_types_error),
                message = stringResource(Res.string.error_check_connection),
                actionText = stringResource(Res.string.retry),
                onAction = { onIntent(RoomTypesIntent.Retry) },
                modifier = Modifier.padding(padding),
            )
            state.catalog.types.isEmpty() -> EmptyState(
                icon = MajesticIcons.category,
                title = stringResource(Res.string.room_types_empty_title),
                message = stringResource(Res.string.room_types_empty),
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
                        state.catalog.types.forEachIndexed { index, type ->
                            if (index > 0) ListDivider()
                            RoomTypeRow(type = type, state = state, onClick = { onIntent(RoomTypesIntent.EditClick(type)) })
                        }
                    }
                }
            }
        }
    }
    state.form?.let { form -> RoomTypeFormSheet(form = form, state = state, onIntent = onIntent) }
}

/** "2 personas · IVA (19 %)", and the rate of a night at the end. */
@Composable
private fun RoomTypeRow(type: RoomType, state: RoomTypesState, onClick: () -> Unit) {
    val inactive = if (type.active) null else stringResource(Res.string.taxes_inactive)
    ListRow(
        title = type.name,
        subtitle = listOfNotNull(inactive, capacityLabel(type.capacity), state.catalog.taxOf(type)?.label()).joinToString(" · "),
        trailing = { AmountText(amount = type.baseRate, style = MajesticTheme.typography.label.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize, fontFeatureSettings = "tnum")) },
        onClick = onClick,
    )
}

@Composable
private fun RoomTypeFormSheet(form: RoomTypeForm, state: RoomTypesState, onIntent: (RoomTypesIntent) -> Unit) {
    TaskSheet(
        title = if (form.typeId == null) stringResource(Res.string.room_types_new) else stringResource(Res.string.room_types_edit, form.name),
        onDismiss = { onIntent(RoomTypesIntent.DismissForm) },
        asDialog = LocalWideWindow.current,
    ) {
        RoomTypeFormFields(form = form, state = state, onIntent = onIntent)
    }
}

/** RF-HAB-01: name, capacity, rate per night with the tax included (decision 3A) and the optional tax. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomTypeFormFields(form: RoomTypeForm, state: RoomTypesState, onIntent: (RoomTypesIntent) -> Unit) {
    fun errorOf(vararg errors: RoomError): RoomError? = form.error.takeIf { it in errors }
    MajesticTextField(
        value = form.name,
        onValueChange = { onIntent(RoomTypesIntent.NameChange(it)) },
        label = stringResource(Res.string.room_types_name),
        help = stringResource(Res.string.room_types_name_hint),
        error = errorOf(RoomError.BlankName, RoomError.DuplicateTypeName)?.let { stringResource(it.message()) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    MajesticTextField(
        value = form.capacityText,
        onValueChange = { onIntent(RoomTypesIntent.CapacityChange(it)) },
        label = stringResource(Res.string.room_types_capacity),
        help = stringResource(Res.string.room_types_capacity_hint),
        error = errorOf(RoomError.InvalidCapacity)?.let { stringResource(it.message()) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    MoneyField(
        digits = form.rateDigits,
        onDigitsChange = { onIntent(RoomTypesIntent.RateChange(it)) },
        label = stringResource(Res.string.room_types_rate),
        help = stringResource(Res.string.room_types_rate_hint),
        error = errorOf(RoomError.InvalidRate)?.let { stringResource(it.message()) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.selectableTaxes.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            FieldLabel(text = stringResource(Res.string.inventory_product_tax))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                MajesticChip(text = stringResource(Res.string.inventory_tax_none), selected = form.taxId == null, onClick = { onIntent(RoomTypesIntent.TaxSelect(null)) })
                state.selectableTaxes.forEach { tax ->
                    MajesticChip(text = tax.label(), selected = form.taxId == tax.id, onClick = { onIntent(RoomTypesIntent.TaxSelect(tax.id)) })
                }
            }
        }
    }
    if (form.typeId != null) {
        MajesticSwitchRow(
            title = stringResource(Res.string.room_types_active),
            help = stringResource(Res.string.room_types_active_hint),
            checked = form.active,
            onCheckedChange = { onIntent(RoomTypesIntent.ActiveChange(it)) },
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End)) {
        MajesticButton(text = stringResource(Res.string.cancel), onClick = { onIntent(RoomTypesIntent.DismissForm) }, style = ButtonStyle.Text)
        MajesticButton(text = stringResource(Res.string.save), onClick = { onIntent(RoomTypesIntent.Save) }, enabled = !state.isSaving, loading = state.isSaving)
    }
}
