package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * An entry and its exit, stored at `Attendance/{id}` (RF-ASI-03 to RF-ASI-05). Each mark keeps the
 * time of the server and the time of the device, so a mark made without connection, or with the
 * clock of the phone wrong, can be told apart (PRD, section 13, 2026-11-20). No coordinates.
 */
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
)

internal fun MarkSource.code(): String = name.lowercase()

private fun markSourceOf(code: String?): MarkSource? = MarkSource.entries.firstOrNull { it.code() == code }

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
    )
}
