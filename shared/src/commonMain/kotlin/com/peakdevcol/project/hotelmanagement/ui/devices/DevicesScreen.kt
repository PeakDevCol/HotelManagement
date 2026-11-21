package com.peakdevcol.project.hotelmanagement.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.formatCalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.devices_authorize
import com.peakdevcol.project.hotelmanagement.resources.devices_authorize_hint
import com.peakdevcol.project.hotelmanagement.resources.devices_authorize_title
import com.peakdevcol.project.hotelmanagement.resources.devices_authorized
import com.peakdevcol.project.hotelmanagement.resources.devices_authorized_on
import com.peakdevcol.project.hotelmanagement.resources.devices_empty_message
import com.peakdevcol.project.hotelmanagement.resources.devices_empty_title
import com.peakdevcol.project.hotelmanagement.resources.devices_error
import com.peakdevcol.project.hotelmanagement.resources.devices_list_title
import com.peakdevcol.project.hotelmanagement.resources.devices_name
import com.peakdevcol.project.hotelmanagement.resources.devices_name_error
import com.peakdevcol.project.hotelmanagement.resources.devices_name_example
import com.peakdevcol.project.hotelmanagement.resources.devices_only_desktop
import com.peakdevcol.project.hotelmanagement.resources.devices_revoke
import com.peakdevcol.project.hotelmanagement.resources.devices_revoke_message
import com.peakdevcol.project.hotelmanagement.resources.devices_revoke_title
import com.peakdevcol.project.hotelmanagement.resources.devices_revoked
import com.peakdevcol.project.hotelmanagement.resources.devices_this_computer
import com.peakdevcol.project.hotelmanagement.resources.devices_this_one_authorized
import com.peakdevcol.project.hotelmanagement.resources.devices_title
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.attendance.message
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ConfirmDialog
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-ASI-11: the computers of reception where the employees mark with their account. */
@Composable
fun DevicesScreen(onBack: () -> Unit, viewModel: DevicesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }

    DevicesContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun DevicesContent(state: DevicesState, onIntent: (DevicesIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.devices_title), onBack = onBack) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.loadFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.devices_error),
                message = stringResource(Res.string.error_check_connection),
                actionText = stringResource(Res.string.retry),
                onAction = { onIntent(DevicesIntent.Retry) },
                modifier = Modifier.padding(padding),
            )
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MajesticSizes.content)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(MajesticSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space4),
                ) {
                    ThisComputer(state = state, onIntent = onIntent)
                    AuthorizedDevices(state = state, onIntent = onIntent)
                }
            }
        }
    }
    state.deviceToRevoke?.let { device ->
        ConfirmDialog(
            title = stringResource(Res.string.devices_revoke_title, device.name),
            message = stringResource(Res.string.devices_revoke_message),
            confirmText = stringResource(Res.string.devices_revoke),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onIntent(DevicesIntent.ConfirmRevoke) },
            onDismiss = { onIntent(DevicesIntent.DismissRevoke) },
            destructive = true,
        )
    }
}

/** Only the desktop app authorizes the computer it runs on, so it is done sitting at reception. */
@Composable
private fun ThisComputer(state: DevicesState, onIntent: (DevicesIntent) -> Unit) {
    val authorized = state.thisDevice
    when {
        !state.canAuthorizeThisDevice -> Banner(tone = BannerTone.Info, icon = MajesticIcons.info, message = stringResource(Res.string.devices_only_desktop))
        authorized != null -> Banner(
            tone = BannerTone.Info,
            icon = MajesticIcons.checkCircle,
            message = stringResource(Res.string.devices_this_one_authorized, authorized.name),
        )
        else -> SectionCard(title = stringResource(Res.string.devices_authorize_title), help = stringResource(Res.string.devices_authorize_hint)) {
            MajesticTextField(
                value = state.nameText,
                onValueChange = { onIntent(DevicesIntent.NameChange(it)) },
                label = stringResource(Res.string.devices_name),
                placeholder = stringResource(Res.string.devices_name_example),
                error = if (state.nameError) stringResource(Res.string.devices_name_error) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            MajesticButton(
                text = stringResource(Res.string.devices_authorize),
                onClick = { onIntent(DevicesIntent.AuthorizeThisDevice) },
                icon = MajesticIcons.computer,
                loading = state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuthorizedDevices(state: DevicesState, onIntent: (DevicesIntent) -> Unit) {
    val active = state.devices.filter { it.active }.sortedBy { it.name.lowercase() }
    if (active.isEmpty()) {
        EmptyState(
            icon = MajesticIcons.computer,
            title = stringResource(Res.string.devices_empty_title),
            message = stringResource(Res.string.devices_empty_message),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space2)) {
        SectionLabel(text = stringResource(Res.string.devices_list_title))
        ListGroup {
            active.forEachIndexed { index, device ->
                if (index > 0) ListDivider()
                DeviceRow(device = device, isThisDevice = device.id == state.thisDeviceId, enabled = !state.isWorking, onRevoke = { onIntent(DevicesIntent.Revoke(device)) })
            }
        }
    }
}

@Composable
private fun DeviceRow(device: AuthorizedDevice, isThisDevice: Boolean, enabled: Boolean, onRevoke: () -> Unit) {
    val details = listOfNotNull(
        if (isThisDevice) stringResource(Res.string.devices_this_computer) else null,
        device.authorizedAt?.let { stringResource(Res.string.devices_authorized_on, formatCalendarDate(operationalDateOf(it, dayStartMinutes = 0))) },
    )
    ListRow(
        title = device.name,
        subtitle = details.joinToString(" · ").ifEmpty { null },
        leading = { MajesticIcon(icon = MajesticIcons.computer, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        trailing = { MajesticButton(text = stringResource(Res.string.devices_revoke), onClick = onRevoke, style = ButtonStyle.Text, enabled = enabled) },
    )
}

private suspend fun DevicesEffect.message(): String = when (this) {
    DevicesEffect.Authorized -> getString(Res.string.devices_authorized)
    DevicesEffect.Revoked -> getString(Res.string.devices_revoked)
    is DevicesEffect.Failed -> error.message()
}
