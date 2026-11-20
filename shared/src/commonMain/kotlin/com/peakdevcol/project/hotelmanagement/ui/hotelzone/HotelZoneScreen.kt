package com.peakdevcol.project.hotelmanagement.ui.hotelzone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.model.formatThousands
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.employee_error_network
import com.peakdevcol.project.hotelmanagement.resources.employee_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_accuracy
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_area_hint
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_area_title
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_error
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_latitude
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_located
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_location_hint
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_location_title
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_longitude
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_none
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_radius
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_saved
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_title
import com.peakdevcol.project.hotelmanagement.resources.hotel_zone_use_my_location
import com.peakdevcol.project.hotelmanagement.resources.order_error_no_session
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.resources.settings_error
import com.peakdevcol.project.hotelmanagement.resources.settings_not_allowed
import com.peakdevcol.project.hotelmanagement.ui.attendance.LocationExplanation
import com.peakdevcol.project.hotelmanagement.ui.attendance.message
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.Banner
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.BannerTone
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonSize
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionCard
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-ASI-10: where the hotel is, and how near it a phone has to be to mark. */
@Composable
fun HotelZoneScreen(onBack: () -> Unit, viewModel: HotelZoneViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> messages.show(effect.message()) }
    }

    HotelZoneContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun HotelZoneContent(state: HotelZoneState, onIntent: (HotelZoneIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.hotel_zone_title), onBack = onBack) },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.loadFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.settings_error),
                message = stringResource(Res.string.error_check_connection),
                actionText = stringResource(Res.string.retry),
                onAction = { onIntent(HotelZoneIntent.Retry) },
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
                    if (!state.hasZone) Banner(tone = BannerTone.Warning, icon = MajesticIcons.warning, message = stringResource(Res.string.hotel_zone_none))
                    HotelLocation(state = state, onIntent = onIntent)
                    ZoneSize(state = state, onIntent = onIntent)
                    if (state.showError) {
                        Text(text = stringResource(Res.string.hotel_zone_error), style = MajesticTheme.typography.caption, color = MajesticTheme.colors.danger)
                    }
                    MajesticButton(
                        text = stringResource(Res.string.save),
                        onClick = { onIntent(HotelZoneIntent.Save) },
                        size = ButtonSize.Large,
                        loading = state.isSaving,
                        enabled = !state.isSaving && !state.isLocating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (state.isExplainingPermission) {
        LocationExplanation(
            onPermissionResult = { granted -> onIntent(HotelZoneIntent.PermissionResult(granted)) },
            onDismiss = { onIntent(HotelZoneIntent.DismissPermission) },
        )
    }
}

/** 3A: "Usar mi ubicación actual" standing at the hotel, or the coordinates copied from a map. */
@Composable
private fun HotelLocation(state: HotelZoneState, onIntent: (HotelZoneIntent) -> Unit) {
    SectionCard(title = stringResource(Res.string.hotel_zone_location_title), help = stringResource(Res.string.hotel_zone_location_hint)) {
        if (state.canUseMyLocation) {
            MajesticButton(
                text = stringResource(Res.string.hotel_zone_use_my_location),
                onClick = { onIntent(HotelZoneIntent.UseMyLocation) },
                style = ButtonStyle.Tonal,
                icon = MajesticIcons.myLocation,
                loading = state.isLocating,
                enabled = !state.isLocating,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
            MajesticTextField(
                value = state.latitudeText,
                onValueChange = { onIntent(HotelZoneIntent.LatitudeChange(it)) },
                label = stringResource(Res.string.hotel_zone_latitude),
                placeholder = LATITUDE_EXAMPLE,
                error = if (state.showError) "" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            MajesticTextField(
                value = state.longitudeText,
                onValueChange = { onIntent(HotelZoneIntent.LongitudeChange(it)) },
                label = stringResource(Res.string.hotel_zone_longitude),
                placeholder = LONGITUDE_EXAMPLE,
                error = if (state.showError) "" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ZoneSize(state: HotelZoneState, onIntent: (HotelZoneIntent) -> Unit) {
    SectionCard(title = stringResource(Res.string.hotel_zone_area_title), help = stringResource(Res.string.hotel_zone_area_hint)) {
        Row(horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
            MajesticTextField(
                value = state.radiusText,
                onValueChange = { onIntent(HotelZoneIntent.RadiusChange(it)) },
                label = stringResource(Res.string.hotel_zone_radius),
                error = if (state.showError) "" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            MajesticTextField(
                value = state.accuracyText,
                onValueChange = { onIntent(HotelZoneIntent.AccuracyChange(it)) },
                label = stringResource(Res.string.hotel_zone_accuracy),
                error = if (state.showError) "" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val LATITUDE_EXAMPLE = "4.60971"
private const val LONGITUDE_EXAMPLE = "-74.08175"

private suspend fun HotelZoneEffect.message(): String = when (this) {
    HotelZoneEffect.Saved -> getString(Res.string.hotel_zone_saved)
    is HotelZoneEffect.Located -> getString(Res.string.hotel_zone_located, formatThousands(accuracyMeters.toLong()))
    is HotelZoneEffect.LocateFailed -> error.message()
    is HotelZoneEffect.SaveFailed -> getString(
        when (error) {
            SettingsError.NoSession -> Res.string.order_error_no_session
            SettingsError.NotAllowed -> Res.string.settings_not_allowed
            SettingsError.Network -> Res.string.employee_error_network
            else -> Res.string.employee_error_unknown
        },
    )
}
