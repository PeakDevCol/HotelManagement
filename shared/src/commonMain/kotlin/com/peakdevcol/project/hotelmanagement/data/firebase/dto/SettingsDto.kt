package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** The parameters stored at `Settings/hotel` (RF-EMP-06). */
@Serializable
internal data class SettingsDto(
    val operationalDayStartMinutes: Int = HotelSettings.DEFAULT_OPERATIONAL_DAY_START_MINUTES,
    val updatedBy: String? = null,
    val updatedAt: BaseTimestamp? = null,
)

internal fun SettingsDto.toSettings() = HotelSettings(
    operationalDayStartMinutes = operationalDayStartMinutes,
    updatedBy = updatedBy,
    updatedAt = (updatedAt as? Timestamp)?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) },
)

internal fun HotelSettings.toDto(updatedBy: String) = SettingsDto(
    operationalDayStartMinutes = operationalDayStartMinutes,
    updatedBy = updatedBy,
    updatedAt = Timestamp.ServerTimestamp,
)
