package com.peakdevcol.project.hotelmanagement.data.location

import com.peakdevcol.project.hotelmanagement.AppConfig
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider

/** The location service of each system, without libraries (PRD, section 13, 2026-11-20). */
internal expect fun locationProvider(config: AppConfig): LocationProvider

/** RF-ASI-11: only the desktop app is set up at the computer of reception to mark for others. */
internal expect fun canMarkForOthersOnThisPlatform(): Boolean
