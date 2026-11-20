package com.peakdevcol.project.hotelmanagement.data.local

import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice

/** RF-ASI-11: the identity of this installation, kept in a small file of the device. */
internal class LocalThisDevice(
    private val store: Lazy<LocalTextStore>,
    override val canMarkForOthers: Boolean,
) : ThisDevice {
    override val id: String by lazy {
        store.value.read()?.trim()?.takeIf { it.isNotEmpty() } ?: newId().also { store.value.write(it) }
    }
}
