package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emits the settings and their changes. Until an administrator saves them, the defaults. */
    fun observeSettings(): Flow<HotelSettings>

    /** Saves the settings, recording who changed them and when (RF-EMP-06). */
    suspend fun save(settings: HotelSettings, updatedBy: String): Result<Unit>
}
