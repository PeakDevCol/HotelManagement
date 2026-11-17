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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.email
import com.peakdevcol.project.hotelmanagement.resources.error_email
import com.peakdevcol.project.hotelmanagement.resources.error_network
import com.peakdevcol.project.hotelmanagement.resources.error_password_length
import com.peakdevcol.project.hotelmanagement.resources.forgot_password
import com.peakdevcol.project.hotelmanagement.resources.forgot_password_sent_message
import com.peakdevcol.project.hotelmanagement.resources.forgot_password_sent_title
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
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                    onValueChange = { viewModel.onIntent(LoginIntent.EmailChange(it)) },
                    label = stringResource(Res.string.email),
                    error = if (state.showEmailError) stringResource(Res.string.error_email) else null,
                    type = FieldType.Email,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                )
                FormTextField(
                    value = state.password,
                    onValueChange = { viewModel.onIntent(LoginIntent.PasswordChange(it)) },
                    label = stringResource(Res.string.password),
                    error = if (state.showPasswordError) stringResource(Res.string.error_password_length) else null,
                    type = FieldType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        viewModel.onIntent(LoginIntent.LoginClick)
                    },
                )
                PrimaryButton(
                    text = stringResource(Res.string.login),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onIntent(LoginIntent.LoginClick)
                    },
                    enabled = state.canSubmit,
                    modifier = Modifier.padding(top = 24.dp),
                )
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onIntent(LoginIntent.ForgotPasswordClick)
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                ) { Text(stringResource(Res.string.forgot_password)) }
            }
        }

        if (state.isLoading) LoadingOverlay()
        state.message?.let { message ->
            val dismiss = { viewModel.onIntent(LoginIntent.MessageDismiss) }
            when (message) {
                is LoginMessage.LoginFailed -> ErrorDialog(stringResource(message.error.messageRes()), onDismiss = dismiss)
                LoginMessage.PasswordEmailFailed -> ErrorDialog(stringResource(Res.string.error_network), onDismiss = dismiss)
                LoginMessage.PasswordEmailRequested -> AlertDialog(
                    onDismissRequest = dismiss,
                    title = { Text(stringResource(Res.string.forgot_password_sent_title)) },
                    text = { Text(stringResource(Res.string.forgot_password_sent_message, state.email.trim())) },
                    confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(Res.string.dialog_accept)) } },
                )
            }
        }
    }
}
