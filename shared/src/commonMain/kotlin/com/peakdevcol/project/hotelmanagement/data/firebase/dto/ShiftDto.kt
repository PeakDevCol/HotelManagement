package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.model.UserArea
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** A shift stored at `Shifts/{id}` (RF-ASI-01). [createdBy] is the administrator who saved it last. */
@Serializable
internal data class ShiftDto(
    val employeeId: String,
    val startAt: Timestamp,
    val endAt: Timestamp,
    /** Null when the shift is in the usual area of the employee. */
    val area: String? = null,
    val createdBy: String,
)

internal fun Shift.toDto(savedBy: String) = ShiftDto(
    employeeId = employeeId,
    startAt = startAt.toTimestamp(),
    endAt = endAt.toTimestamp(),
    area = area?.code(),
    createdBy = savedBy,
)

internal fun ShiftDto.toShift(id: String) = Shift(
    id = id,
    employeeId = employeeId,
    startAt = startAt.toInstant(),
    endAt = endAt.toInstant(),
    area = area?.let { code -> UserArea.entries.firstOrNull { it.code() == code } },
)
