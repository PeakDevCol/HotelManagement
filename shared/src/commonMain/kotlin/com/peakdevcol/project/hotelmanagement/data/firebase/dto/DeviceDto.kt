package com.peakdevcol.project.hotelmanagement.data.firebase.dto

import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** A computer allowed to mark attendance, stored at `Devices/{id}` (RF-ASI-11). */
@Serializable
internal data class DeviceDto(
    val name: String,
    val authorizedBy: String,
    val authorizedAt: BaseTimestamp? = null,
    val active: Boolean,
)

internal fun AuthorizedDevice.toDto() = DeviceDto(name = name, authorizedBy = authorizedBy, authorizedAt = Timestamp.ServerTimestamp, active = active)

internal fun DeviceDto.toDevice(id: String) = AuthorizedDevice(
    id = id,
    name = name,
    authorizedBy = authorizedBy,
    authorizedAt = (authorizedAt as? Timestamp)?.toInstant(),
    active = active,
)
