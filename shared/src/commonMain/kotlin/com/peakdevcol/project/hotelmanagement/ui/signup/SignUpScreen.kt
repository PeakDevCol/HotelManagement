package com.peakdevcol.project.hotelmanagement.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.confirm_password
import com.peakdevcol.project.hotelmanagement.resources.create_account
import com.peakdevcol.project.hotelmanagement.resources.email
import com.peakdevcol.project.hotelmanagement.resources.error_email
import com.peakdevcol.project.hotelmanagement.resources.error_full_name
import com.peakdevcol.project.hotelmanagement.resources.error_password_length
import com.peakdevcol.project.hotelmanagement.resources.error_password_mismatch
import com.peakdevcol.project.hotelmanagement.resources.full_name
import com.peakdevcol.project.hotelmanagement.resources.password
import com.peakdevcol.project.hotelmanagement.ui.components.BackButton
import com.peakdevcol.project.hotelmanagement.ui.components.ErrorDialog
import com.peakdevcol.project.hotelmanagement.ui.components.FieldType
import com.peakdevcol.project.hotelmanagement.ui.components.FormTextField
import com.peakdevcol.project.hotelmanagement.ui.components.LoadingOverlay
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.components.messageRes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpScreen(onBack: () -> Unit, viewModel: SignUpViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val next = { focusManager.moveFocus(FocusDirection.Down); Unit }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                BackButton(onClick = onBack)
                FormTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = stringResource(Res.string.full_name),
                    error = if (state.showFullNameError) stringResource(Res.string.error_full_name) else null,
                    onImeAction = next,
                )
                FormTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(Res.string.email),
                    error = if (state.showEmailError) stringResource(Res.string.error_email) else null,
                    type = FieldType.Email,
                    onImeAction = next,
                )
                FormTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(Res.string.password),
                    error = if (state.showPasswordError) stringResource(Res.string.error_password_length) else null,
                    type = FieldType.Password,
                    onImeAction = next,
                )
                FormTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = stringResource(Res.string.confirm_password),
                    error = if (state.showConfirmPasswordError) stringResource(Res.string.error_password_mismatch) else null,
                    type = FieldType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        viewModel.onCreateAccountClick()
                    },
                )
                PrimaryButton(
                    text = stringResource(Res.string.create_account),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onCreateAccountClick()
                    },
                    enabled = state.canSubmit,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        if (state.isLoading) LoadingOverlay()
        state.error?.let { error ->
            ErrorDialog(message = stringResource(error.messageRes()), onDismiss = viewModel::onErrorDismissed)
        }
    }
}
