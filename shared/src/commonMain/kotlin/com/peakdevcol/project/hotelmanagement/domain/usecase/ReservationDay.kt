package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.CalendarDate
import com.peakdevcol.project.hotelmanagement.domain.model.RoomType
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.operationalDateOf
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * The day a reservation can start from: the operational day (P17), so a guest who arrives after
 * midnight still takes the night that began the day before.
 */
internal suspend fun SettingsRepository.reservationToday(clock: Clock): CalendarDate =
    operationalDateOf(clock.now(), observeSettings().first().operationalDayStartMinutes)

/** The tax the rate of [type] includes, if it is still active, like the products (RF-INV-07). */
internal fun List<Tax>.rateOf(type: RoomType): Int = firstOrNull { it.id == type.taxId }?.takeIf { it.active }?.rate ?: 0
