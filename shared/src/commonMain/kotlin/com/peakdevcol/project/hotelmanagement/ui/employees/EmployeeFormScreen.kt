package com.peakdevcol.project.hotelmanagement.ui.employees

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeCreation
import com.peakdevcol.project.hotelmanagement.domain.model.EmployeeError
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import com.peakdevcol.project.hotelmanagement.domain.model.UserType
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.email
import com.peakdevcol.project.hotelmanagement.resources.employee_active
import com.peakdevcol.project.hotelmanagement.resources.employee_active_hint
import com.peakdevcol.project.hotelmanagement.resources.employee_area
import com.peakdevcol.project.hotelmanagement.resources.employee_created
import com.peakdevcol.project.hotelmanagement.resources.employee_created_without_email
import com.peakdevcol.project.hotelmanagement.resources.employee_email_hint
import com.peakdevcol.project.hotelmanagement.resources.employee_name
import com.peakdevcol.project.hotelmanagement.resources.employee_not_found
import com.peakdevcol.project.hotelmanagement.resources.employee_own_access_hint
import com.peakdevcol.project.hotelmanagement.resources.employee_password_email_sent
import com.peakdevcol.project.hotelmanagement.resources.employee_role
import com.peakdevcol.project.hotelmanagement.resources.employee_saved
import com.peakdevcol.project.hotelmanagement.resources.employee_send_password_email
import com.peakdevcol.project.hotelmanagement.resources.employees_back_to_list
import com.peakdevcol.project.hotelmanagement.resources.employees_edit
import com.peakdevcol.project.hotelmanagement.resources.employees_new
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.FieldLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticChip
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticSwitchRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SegmentedControl
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

/** RF-EMP-02 to RF-EMP-05. A null [employeeId] creates an employee. */
@Composable
fun EmployeeFormScreen(
    employeeId: String?,
    onBack: () -> Unit,
    viewModel: EmployeeFormViewModel = koinViewModel(key = employeeId ?: NEW_EMPLOYEE_KEY) { parametersOf(employeeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            messages.show(effect.message())
            // A new employee has to know the email is on its way, so the message goes first.
            if (effect is EmployeeFormEffect.Created || effect is EmployeeFormEffect.Saved) onBack()
        }
    }

    EmployeeFormContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun EmployeeFormContent(state: EmployeeFormState, onIntent: (EmployeeFormIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(if (state.isNew) Res.string.employees_new else Res.string.employees_edit), onBack = onBack) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.notFound -> EmptyState(
                icon = MajesticIcons.person,
                title = stringResource(Res.string.employee_not_found),
                message = stringResource(Res.string.employees_back_to_list),
                modifier = Modifier.padding(padding),
            )
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                EmployeeForm(state = state, onIntent = onIntent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeForm(state: EmployeeFormState, onIntent: (EmployeeFormIntent) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = MajesticSizes.content)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(MajesticSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space5),
    ) {
        MajesticTextField(
            value = state.fullName,
            onValueChange = { onIntent(EmployeeFormIntent.NameChange(it)) },
            label = stringResource(Res.string.employee_name),
            error = state.fieldError.takeIf { it == EmployeeError.InvalidName }?.let { stringResource(it.message()) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isNew) {
            MajesticTextField(
                value = state.email,
                onValueChange = { onIntent(EmployeeFormIntent.EmailChange(it)) },
                label = stringResource(Res.string.email),
                help = stringResource(Res.string.employee_email_hint),
                error = state.fieldError.takeIf { it == EmployeeError.InvalidEmail || it == EmployeeError.EmailAlreadyInUse }?.let { stringResource(it.message()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column {
                FieldLabel(text = stringResource(Res.string.email))
                Text(text = state.email, style = MajesticTheme.typography.bodyLarge, color = MajesticTheme.colors.ink)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            FieldLabel(text = stringResource(Res.string.employee_role))
            SegmentedControl(
                options = UserType.entries,
                selected = state.type,
                onSelect = { if (state.canChangeAccess) onIntent(EmployeeFormIntent.TypeSelect(it)) },
                label = { stringResource(it.label()) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
            FieldLabel(text = stringResource(Res.string.employee_area))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2), verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
                UserArea.entries.forEach { area ->
                    MajesticChip(text = stringResource(area.label()), selected = state.area == area, onClick = { onIntent(EmployeeFormIntent.AreaSelect(area)) })
                }
            }
        }
        if (!state.isNew) {
            MajesticSwitchRow(
                title = stringResource(Res.string.employee_active),
                help = stringResource(Res.string.employee_active_hint),
                checked = state.active,
                onCheckedChange = { onIntent(EmployeeFormIntent.ActiveChange(it)) },
                enabled = state.canChangeAccess,
            )
        }
        if (state.isOwnProfile) {
            Text(text = stringResource(Res.string.employee_own_access_hint), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.inkMuted)
        }
        MajesticButton(
            text = stringResource(Res.string.save),
            onClick = { onIntent(EmployeeFormIntent.Save) },
            size = ButtonSize.Large,
            enabled = state.canSave,
            loading = state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.isNew) {
            MajesticButton(
                text = stringResource(Res.string.employee_send_password_email),
                onClick = { onIntent(EmployeeFormIntent.SendPasswordEmail) },
                style = ButtonStyle.Outline,
                icon = MajesticIcons.mail,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private suspend fun EmployeeFormEffect.message(): String = when (this) {
    is EmployeeFormEffect.Created -> getString(
        when (creation) {
            EmployeeCreation.INVITED -> Res.string.employee_created
            EmployeeCreation.CREATED_WITHOUT_EMAIL -> Res.string.employee_created_without_email
        },
    )
    EmployeeFormEffect.Saved -> getString(Res.string.employee_saved)
    EmployeeFormEffect.PasswordEmailSent -> getString(Res.string.employee_password_email_sent)
    is EmployeeFormEffect.Failed -> getString(error.message())
}

private const val NEW_EMPLOYEE_KEY = "new-employee"
