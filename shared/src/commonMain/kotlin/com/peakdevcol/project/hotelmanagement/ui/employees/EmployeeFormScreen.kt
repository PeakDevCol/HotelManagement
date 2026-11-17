package com.peakdevcol.project.hotelmanagement.ui.employees

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import com.peakdevcol.project.hotelmanagement.resources.employees_edit
import com.peakdevcol.project.hotelmanagement.resources.employees_new
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.components.FieldType
import com.peakdevcol.project.hotelmanagement.ui.components.FormTextField
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EmployeeFormScreen(
    employeeId: String?,
    onBack: () -> Unit,
    viewModel: EmployeeFormViewModel = koinViewModel(key = employeeId ?: NEW_EMPLOYEE_KEY) { parametersOf(employeeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            snackbarHostState.showSnackbar(effect.message())
            // The message is read first: a new employee has to know the email is on its way.
            if (effect is EmployeeFormEffect.Created || effect is EmployeeFormEffect.Saved) onBack()
        }
    }

    val title = if (state.isNew) Res.string.employees_new else Res.string.employees_edit
    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.notFound -> CenteredMessage(stringResource(Res.string.employee_not_found), Modifier.padding(padding))
            else -> EmployeeForm(state = state, onIntent = viewModel::onIntent, modifier = Modifier.padding(padding))
        }
    }

    if (state.isWorking) LoadingOverlay()
}

@Composable
private fun EmployeeForm(state: EmployeeFormState, onIntent: (EmployeeFormIntent) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                value = state.fullName,
                onValueChange = { onIntent(EmployeeFormIntent.NameChange(it)) },
                label = stringResource(Res.string.employee_name),
                error = fieldError(state.fieldError, EmployeeError.InvalidName),
            )
            if (state.isNew) {
                FormTextField(
                    value = state.email,
                    onValueChange = { onIntent(EmployeeFormIntent.EmailChange(it)) },
                    label = stringResource(Res.string.email),
                    error = fieldError(state.fieldError, EmployeeError.InvalidEmail)
                        ?: fieldError(state.fieldError, EmployeeError.EmailAlreadyInUse),
                    type = FieldType.Email,
                )
                Hint(stringResource(Res.string.employee_email_hint))
            } else {
                Text(text = state.email, style = MaterialTheme.typography.bodyLarge)
            }
            Options(
                title = stringResource(Res.string.employee_role),
                options = UserType.entries,
                selected = state.type,
                label = { stringResource(it.label()) },
                enabled = state.canChangeAccess,
                onSelect = { onIntent(EmployeeFormIntent.TypeSelect(it)) },
            )
            Options(
                title = stringResource(Res.string.employee_area),
                options = UserArea.entries,
                selected = state.area,
                label = { stringResource(it.label()) },
                enabled = true,
                onSelect = { onIntent(EmployeeFormIntent.AreaSelect(it)) },
            )
            if (!state.isNew) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.employee_active), style = MaterialTheme.typography.bodyLarge)
                        Hint(stringResource(Res.string.employee_active_hint))
                    }
                    Switch(
                        checked = state.active,
                        onCheckedChange = { onIntent(EmployeeFormIntent.ActiveChange(it)) },
                        enabled = state.canChangeAccess,
                    )
                }
                if (state.isOwnProfile) Hint(stringResource(Res.string.employee_own_access_hint))
            }
            PrimaryButton(
                text = stringResource(Res.string.save),
                onClick = { onIntent(EmployeeFormIntent.Save) },
                enabled = state.canSave,
            )
            if (!state.isNew) {
                TextButton(
                    onClick = { onIntent(EmployeeFormIntent.SendPasswordEmail) },
                    enabled = state.canSave,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(Res.string.employee_send_password_email)) }
            }
        }
    }
}

@Composable
private fun <T> Options(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, onClick = { onSelect(option) }, enabled = enabled, role = Role.RadioButton)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null, enabled = enabled)
                Text(label(option), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun fieldError(current: EmployeeError?, expected: EmployeeError): String? =
    if (current == expected) stringResource(expected.message()) else null

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
