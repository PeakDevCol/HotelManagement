package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * An entry and its exit, stored at `Attendance/{id}` (RF-ASI-03 to RF-ASI-05). Each mark keeps the
 * time of the server and the time of the device, so a mark made without connection, or with the
 * clock of the phone wrong, can be told apart (PRD, section 13, 2026-11-20). No coordinates.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class AttendanceDto(
    val employeeId: String,
    val clockInAt: BaseTimestamp? = null,
    val clockInDeviceTime: Timestamp,
    val clockInSource: String,
    val clockInDeviceId: String? = null,
    val clockInDistanceMeters: Int? = null,
    val clockInAccuracyMeters: Int? = null,
    /** Null while the entry is open: the query of the open entry looks for it. */
    val clockOutAt: BaseTimestamp? = null,
    val clockOutDeviceTime: Timestamp? = null,
    val clockOutSource: String? = null,
    val clockOutDeviceId: String? = null,
    val clockOutDistanceMeters: Int? = null,
    val clockOutAccuracyMeters: Int? = null,
    /**
     * RF-ASI-07: the corrections of the administrator, by id. A map and not a list, because the
     * time of the server can go inside a map but not inside a list. The entry of an employee
     * leaves these fields out, as the rules require.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val corrections: Map<String, CorrectionDto> = emptyMap(),
    /** The correction just added, so the rules can check it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val lastCorrectionId: String? = null,
)

/** RF-ASI-07: one correction inside `Attendance/{id}.corrections`. It is written once and never changes. */
@Serializable
internal data class CorrectionDto(
    val kind: String,
    /** `clockIn` or `clockOut`; null for a registration. */
    val mark: String? = null,
    val time: Timestamp? = null,
    val reason: String? = null,
    val by: String,
    val at: BaseTimestamp = Timestamp.ServerTimestamp,
)

internal fun MarkSource.code(): String = name.lowercase()

private fun markSourceOf(code: String?): MarkSource? = MarkSource.entries.firstOrNull { it.code() == code }

internal fun CorrectionKind.code(): String = when (this) {
    CorrectionKind.DEVICE_TIME -> "device"
    CorrectionKind.SERVER_TIME -> "server"
    CorrectionKind.OTHER_TIME -> "other"
    CorrectionKind.REGISTRATION -> "registration"
}

/** The first part of the names of the fields of a mark: `clockIn` or `clockOut`. */
internal fun MarkKind.fieldPrefix(): String = when (this) {
    MarkKind.ENTRY -> "clockIn"
    MarkKind.EXIT -> "clockOut"
}

/** A new entry: the server puts its own time. */
internal fun AttendanceRecord.toDto() = AttendanceDto(
    employeeId = employeeId,
    clockInAt = Timestamp.ServerTimestamp,
    clockInDeviceTime = clockIn.deviceTime.toTimestamp(),
    clockInSource = clockIn.source.code(),
    clockInDeviceId = clockIn.deviceId,
    clockInDistanceMeters = clockIn.distanceMeters,
    clockInAccuracyMeters = clockIn.accuracyMeters,
)

/** RF-ASI-07 with decision 4A: a record the administrator wrote, with the times they gave and why. */
internal fun AttendanceRecord.toRegistrationDto(): AttendanceDto {
    val exit = requireNotNull(clockOut) { "A registration has its exit" }
    val registration = corrections.single()
    return AttendanceDto(
        employeeId = employeeId,
        clockInAt = clockIn.deviceTime.toTimestamp(),
        clockInDeviceTime = clockIn.deviceTime.toTimestamp(),
        clockInSource = clockIn.source.code(),
        clockOutAt = exit.deviceTime.toTimestamp(),
        clockOutDeviceTime = exit.deviceTime.toTimestamp(),
        clockOutSource = exit.source.code(),
        corrections = mapOf(registration.id to registration.toDto()),
        lastCorrectionId = registration.id,
    )
}

/** The time of the server is put by the server, never by the device of the administrator. */
internal fun Correction.toDto() = CorrectionDto(
    kind = kind.code(),
    mark = mark?.fieldPrefix(),
    time = time?.toTimestamp(),
    reason = reason,
    by = by,
)

/** A server time not known yet reads as null: the mark has not reached the server. */
internal fun AttendanceDto.toRecord(id: String): AttendanceRecord? {
    val inSource = markSourceOf(clockInSource) ?: return null
    val clockOut = clockOutDeviceTime?.let { deviceTime ->
        Mark(
            at = (clockOutAt as? Timestamp)?.toInstant(),
            deviceTime = deviceTime.toInstant(),
            source = markSourceOf(clockOutSource) ?: return null,
            deviceId = clockOutDeviceId,
            distanceMeters = clockOutDistanceMeters,
            accuracyMeters = clockOutAccuracyMeters,
        )
    }
    return AttendanceRecord(
        id = id,
        employeeId = employeeId,
        clockIn = Mark(
            at = (clockInAt as? Timestamp)?.toInstant(),
            deviceTime = clockInDeviceTime.toInstant(),
            source = inSource,
            deviceId = clockInDeviceId,
            distanceMeters = clockInDistanceMeters,
            accuracyMeters = clockInAccuracyMeters,
        ),
        clockOut = clockOut,
        // The last correction is the one that counts; one that has not reached the server is the newest.
        corrections = corrections
            .mapNotNull { (correctionId, correction) -> correction.toCorrection(correctionId) }
            .sortedWith(compareBy(nullsLast()) { it.at }),
    )
}

/** Null when its kind or its mark is not one the app knows. */
internal fun CorrectionDto.toCorrection(id: String): Correction? {
    val correctionKind = CorrectionKind.entries.firstOrNull { it.code() == kind } ?: return null
    val markKind = mark?.let { prefix -> MarkKind.entries.firstOrNull { it.fieldPrefix() == prefix } ?: return null }
    return Correction(
        id = id,
        kind = correctionKind,
        mark = markKind,
        time = time?.toInstant(),
        reason = reason,
        by = by,
        at = (at as? Timestamp)?.toInstant(),
    )
}
