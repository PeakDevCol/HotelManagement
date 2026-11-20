package com.peakdevcol.project.hotelmanagement.ui.attendance

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaTime
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.email
import com.peakdevcol.project.hotelmanagement.resources.password
import com.peakdevcol.project.hotelmanagement.resources.password_hide
import com.peakdevcol.project.hotelmanagement.resources.password_show
import com.peakdevcol.project.hotelmanagement.resources.reception_entry_done
import com.peakdevcol.project.hotelmanagement.resources.reception_exit_done
import com.peakdevcol.project.hotelmanagement.resources.reception_failed
import com.peakdevcol.project.hotelmanagement.resources.reception_hint
import com.peakdevcol.project.hotelmanagement.resources.reception_mark
import com.peakdevcol.project.hotelmanagement.resources.reception_title
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import org.jetbrains.compose.resources.stringResource

/** RF-ASI-11: on the authorized computer of reception, each employee marks with their own account. */
@Composable
fun ReceptionMarkingCard(state: ReceptionMarkingState, onIntent: (ReceptionMarkingIntent) -> Unit) {
    val focusManager = LocalFocusManager.current
    val mark = { if (state.canMark) onIntent(ReceptionMarkingIntent.Mark) }
    SectionCard(title = stringResource(Res.string.reception_title), help = stringResource(Res.string.reception_hint)) {
        MajesticTextField(
            value = state.emailText,
            onValueChange = { onIntent(ReceptionMarkingIntent.EmailChange(it)) },
            label = stringResource(Res.string.email),
            enabled = !state.isMarking,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        MajesticTextField(
            value = state.passwordText,
            onValueChange = { onIntent(ReceptionMarkingIntent.PasswordChange(it)) },
            label = stringResource(Res.string.password),
            enabled = !state.isMarking,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { mark() }),
            trailing = {
                MajesticIconButton(
                    icon = if (state.isPasswordVisible) MajesticIcons.visibilityOff else MajesticIcons.visibility,
                    contentDescription = stringResource(if (state.isPasswordVisible) Res.string.password_hide else Res.string.password_show),
                    onClick = { onIntent(ReceptionMarkingIntent.TogglePasswordVisibility) },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        MajesticButton(
            text = stringResource(Res.string.reception_mark),
            onClick = mark,
            size = ButtonSize.Large,
            icon = MajesticIcons.attendance,
            enabled = state.canMark,
            loading = state.isMarking,
            modifier = Modifier.fillMaxWidth(),
        )
        state.result?.let { ReceptionResultBanner(it) }
    }
}

@Composable
private fun ReceptionResultBanner(result: ReceptionResult) {
    when (result) {
        is ReceptionResult.Done -> {
            val outcome = result.outcome
            val name = outcome.employeeName.orEmpty()
            val time = formatBogotaTime(outcome.time)
            Banner(
                tone = BannerTone.Info,
                icon = MajesticIcons.checkCircle,
                message = if (outcome.kind == MarkKind.ENTRY) {
                    stringResource(Res.string.reception_entry_done, name, time)
                } else {
                    stringResource(Res.string.reception_exit_done, name, time, outcome.workedTime?.let { durationLabel(it) }.orEmpty())
                },
            )
        }
        is ReceptionResult.Failed -> Banner(
            tone = BannerTone.Danger,
            icon = MajesticIcons.error,
            title = stringResource(Res.string.reception_failed),
            message = result.error.text(),
        )
    }
}
