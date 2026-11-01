package com.peakdevcol.project.hotelmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.AuthError
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.back
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.dialog_title_oops
import com.peakdevcol.project.hotelmanagement.resources.error_email_in_use
import com.peakdevcol.project.hotelmanagement.resources.error_invalid_credentials
import com.peakdevcol.project.hotelmanagement.resources.error_network
import com.peakdevcol.project.hotelmanagement.resources.error_unknown
import com.peakdevcol.project.hotelmanagement.resources.error_weak_password
import com.peakdevcol.project.hotelmanagement.resources.ic_arrow_back
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class FieldType { Text, Email, Password }

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    type: FieldType = FieldType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (type == FieldType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (type) {
                FieldType.Text -> KeyboardType.Text
                FieldType.Email -> KeyboardType.Email
                FieldType.Password -> KeyboardType.Password
            },
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(48.dp)) {
        Text(text.uppercase())
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
        Text(stringResource(Res.string.back))
    }
}

/** Blocks the screen while a request is running. */
@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_oops)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_accept)) }
        },
    )
}

fun AuthError.messageRes(): StringResource = when (this) {
    AuthError.InvalidCredentials -> Res.string.error_invalid_credentials
    AuthError.EmailAlreadyInUse -> Res.string.error_email_in_use
    AuthError.WeakPassword -> Res.string.error_weak_password
    AuthError.Network -> Res.string.error_network
    is AuthError.Unknown -> Res.string.error_unknown
}
