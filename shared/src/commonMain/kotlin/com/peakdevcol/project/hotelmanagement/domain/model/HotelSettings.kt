package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.time.Instant

const val MINUTES_PER_DAY = 24 * 60

/** RF-EMP-06: the values of the operation that an administrator can change. Each version adds its own. */
data class HotelSettings(
    /**
     * When the operational day starts, in minutes after midnight in Colombian time. Sales after
     * midnight belong to the day before until this time (P17).
     */
    val operationalDayStartMinutes: Int = DEFAULT_OPERATIONAL_DAY_START_MINUTES,
    /** RF-ASI-08: how many minutes after the start of a shift an entry still counts as on time. */
    val lateToleranceMinutes: Int = DEFAULT_LATE_TOLERANCE_MINUTES,
    /** RF-ASI-06: after how many hours an entry without exit is marked as a forgotten exit. */
    val maxOpenEntryHours: Int = DEFAULT_MAX_OPEN_ENTRY_HOURS,
    /** Who made the last change and when. Both are null while nobody has changed the defaults. */
    val updatedBy: String? = null,
    val updatedAt: Instant? = null,
) {
    companion object {
        const val DEFAULT_OPERATIONAL_DAY_START_MINUTES = 6 * 60
        const val DEFAULT_LATE_TOLERANCE_MINUTES = 10
        const val DEFAULT_MAX_OPEN_ENTRY_HOURS = 16
        val LATE_TOLERANCE_RANGE = 0..120
        val MAX_OPEN_ENTRY_HOURS_RANGE = 1..24
    }
}

/** The settings with the name of who changed them last, when their profile can be read. */
data class HotelSettingsDetail(
    val settings: HotelSettings,
    val updatedByName: String?,
)

sealed class SettingsError(message: String) : Exception(message) {
    data object InvalidTime : SettingsError("The time is not a valid time of the day")
    data object InvalidAttendanceRules : SettingsError("The tolerance or the hours are out of range")
    data object NoSession : SettingsError("There is no logged-in employee")
    data object NotAllowed : SettingsError("Only an administrator changes the settings")
    data object Network : SettingsError("The server could not be reached")
    data object Unknown : SettingsError("Unknown error")
}

/** RF-EMP-07: the result of a manual backup. */
data class BackupSummary(
    val filePath: String,
    val documentCount: Int,
)

sealed class BackupError(message: String) : Exception(message) {
    data object NotAvailable : BackupError("Backups are made from the desktop app")
    data object InvalidDate : BackupError("The date is not a valid date")
    data object NotAllowed : BackupError("Only an administrator makes backups")
    data object Network : BackupError("The server could not be reached")
    data object CannotWriteFile : BackupError("The backup file could not be written")
    data object Unknown : BackupError("Unknown error")
}
