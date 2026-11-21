package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** The parameters stored at `Settings/hotel` (RF-EMP-06). Each phase adds its own, with a default. */
@Serializable
internal data class SettingsDto(
    val operationalDayStartMinutes: Int = HotelSettings.DEFAULT_OPERATIONAL_DAY_START_MINUTES,
    val lateToleranceMinutes: Int = HotelSettings.DEFAULT_LATE_TOLERANCE_MINUTES,
    val maxOpenEntryHours: Int = HotelSettings.DEFAULT_MAX_OPEN_ENTRY_HOURS,
    val hotelLatitude: Double? = null,
    val hotelLongitude: Double? = null,
    val zoneRadiusMeters: Int = HotelSettings.DEFAULT_ZONE_RADIUS_METERS,
    val minAccuracyMeters: Int = HotelSettings.DEFAULT_MIN_ACCURACY_METERS,
    val updatedBy: String? = null,
    val updatedAt: BaseTimestamp? = null,
)

internal fun SettingsDto.toSettings() = HotelSettings(
    operationalDayStartMinutes = operationalDayStartMinutes,
    lateToleranceMinutes = lateToleranceMinutes,
    maxOpenEntryHours = maxOpenEntryHours,
    hotelLatitude = hotelLatitude,
    hotelLongitude = hotelLongitude,
    zoneRadiusMeters = zoneRadiusMeters,
    minAccuracyMeters = minAccuracyMeters,
    updatedBy = updatedBy,
    updatedAt = (updatedAt as? Timestamp)?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) },
)

internal fun HotelSettings.toDto(updatedBy: String) = SettingsDto(
    operationalDayStartMinutes = operationalDayStartMinutes,
    lateToleranceMinutes = lateToleranceMinutes,
    maxOpenEntryHours = maxOpenEntryHours,
    hotelLatitude = hotelLatitude,
    hotelLongitude = hotelLongitude,
    zoneRadiusMeters = zoneRadiusMeters,
    minAccuracyMeters = minAccuracyMeters,
    updatedBy = updatedBy,
    updatedAt = Timestamp.ServerTimestamp,
)
