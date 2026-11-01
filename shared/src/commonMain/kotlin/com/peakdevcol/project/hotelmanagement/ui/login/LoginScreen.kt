package com.peakdevcol.project.hotelmanagement.ui.login

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
import com.peakdevcol.project.hotelmanagement.resources.email
import com.peakdevcol.project.hotelmanagement.resources.error_email
import com.peakdevcol.project.hotelmanagement.resources.error_password_length
import com.peakdevcol.project.hotelmanagement.resources.login
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
fun LoginScreen(onBack: () -> Unit, viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

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
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(Res.string.email),
                    error = if (state.showEmailError) stringResource(Res.string.error_email) else null,
                    type = FieldType.Email,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                )
                FormTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(Res.string.password),
                    error = if (state.showPasswordError) stringResource(Res.string.error_password_length) else null,
                    type = FieldType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        viewModel.onLoginClick()
                    },
                )
                PrimaryButton(
                    text = stringResource(Res.string.login),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onLoginClick()
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
