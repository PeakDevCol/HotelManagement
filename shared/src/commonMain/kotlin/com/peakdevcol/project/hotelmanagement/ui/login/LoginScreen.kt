package com.peakdevcol.project.hotelmanagement.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.email
import com.peakdevcol.project.hotelmanagement.resources.error_email
import com.peakdevcol.project.hotelmanagement.resources.error_email_in_use
import com.peakdevcol.project.hotelmanagement.resources.error_invalid_credentials
import com.peakdevcol.project.hotelmanagement.resources.error_network
import com.peakdevcol.project.hotelmanagement.resources.error_password_length
import com.peakdevcol.project.hotelmanagement.resources.error_unknown
import com.peakdevcol.project.hotelmanagement.resources.error_weak_password
import com.peakdevcol.project.hotelmanagement.resources.forgot_password
import com.peakdevcol.project.hotelmanagement.resources.forgot_password_sent_message
import com.peakdevcol.project.hotelmanagement.resources.forgot_password_sent_title
import com.peakdevcol.project.hotelmanagement.resources.login
import com.peakdevcol.project.hotelmanagement.resources.login_no_account
import com.peakdevcol.project.hotelmanagement.resources.login_subtitle
import com.peakdevcol.project.hotelmanagement.resources.login_title
import com.peakdevcol.project.hotelmanagement.resources.password
import com.peakdevcol.project.hotelmanagement.resources.password_hide
import com.peakdevcol.project.hotelmanagement.resources.password_show
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.HotelBrandHeader
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-AUTH-01 and RF-AUTH-04: the first screen without a session. */
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun LoginContent(state: LoginState, onIntent: (LoginIntent) -> Unit) {
    val focusManager = LocalFocusManager.current
    val logIn = {
        focusManager.clearFocus()
        onIntent(LoginIntent.LoginClick)
    }
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = MajesticSizes.mediumWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MajesticSpacing.space6, vertical = MajesticSpacing.space10),
            verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
        ) {
            HotelBrandHeader()
            Column {
                Text(text = stringResource(Res.string.login_title), style = MajesticTheme.typography.headline, color = MajesticTheme.colors.ink)
                Text(text = stringResource(Res.string.login_subtitle), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            }
            MajesticTextField(
                value = state.email,
                onValueChange = { onIntent(LoginIntent.EmailChange(it)) },
                label = stringResource(Res.string.email),
                error = if (state.showEmailError) stringResource(Res.string.error_email) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            MajesticTextField(
                value = state.password,
                onValueChange = { onIntent(LoginIntent.PasswordChange(it)) },
                label = stringResource(Res.string.password),
                error = if (state.showPasswordError) stringResource(Res.string.error_password_length) else null,
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { logIn() }),
                trailing = {
                    MajesticIconButton(
                        icon = if (state.isPasswordVisible) MajesticIcons.visibilityOff else MajesticIcons.visibility,
                        contentDescription = stringResource(if (state.isPasswordVisible) Res.string.password_hide else Res.string.password_show),
                        onClick = { onIntent(LoginIntent.TogglePasswordVisibility) },
                    )
                },
            )
            state.message?.let { message -> LoginBanner(message = message, email = state.email.trim()) }
            MajesticButton(
                text = stringResource(Res.string.login),
                onClick = logIn,
                size = ButtonSize.Large,
                enabled = state.canSubmit,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            MajesticButton(
                text = stringResource(Res.string.forgot_password),
                onClick = {
                    focusManager.clearFocus()
                    onIntent(LoginIntent.ForgotPasswordClick)
                },
                style = ButtonStyle.Text,
                enabled = !state.isLoading,
            )
            Text(
                text = stringResource(Res.string.login_no_account),
                style = MajesticTheme.typography.caption,
                color = MajesticTheme.colors.inkMuted,
                modifier = Modifier.padding(top = MajesticSpacing.space6),
            )
        }
    }
}

@Composable
private fun LoginBanner(message: LoginMessage, email: String) {
    when (message) {
        is LoginMessage.LoginFailed -> Banner(tone = BannerTone.Danger, message = stringResource(message.error.messageRes()))
        LoginMessage.PasswordEmailFailed -> Banner(tone = BannerTone.Danger, message = stringResource(Res.string.error_network))
        LoginMessage.PasswordEmailRequested -> Banner(
            tone = BannerTone.Info,
            title = stringResource(Res.string.forgot_password_sent_title),
            message = stringResource(Res.string.forgot_password_sent_message, email),
            icon = MajesticIcons.mail,
        )
    }
}

private fun AuthError.messageRes(): StringResource = when (this) {
    AuthError.InvalidCredentials -> Res.string.error_invalid_credentials
    AuthError.EmailAlreadyInUse -> Res.string.error_email_in_use
    AuthError.WeakPassword -> Res.string.error_weak_password
    AuthError.Network -> Res.string.error_network
    is AuthError.Unknown -> Res.string.error_unknown
}
