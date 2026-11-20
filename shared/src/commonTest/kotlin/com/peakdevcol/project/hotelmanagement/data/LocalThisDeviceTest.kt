package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.local.LocalTextStore
import com.peakdevcol.project.hotelmanagement.data.local.LocalThisDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RF-ASI-11: the authorization of a computer survives restarting the app. */
class LocalThisDeviceTest {
    private class MemoryTextStore(var text: String? = null) : LocalTextStore {
        override fun read(): String? = text

        override fun write(text: String) {
            this.text = text
        }
    }

    @Test
    fun theIdIsCreatedOnceAndKept() {
        val store = MemoryTextStore()

        val first = LocalThisDevice(lazy { store }, canMarkForOthers = true).id
        val again = LocalThisDevice(lazy { store }, canMarkForOthers = true).id

        assertTrue(first.isNotBlank())
        assertEquals(first, again)
        assertEquals(first, store.text)
    }

    @Test
    fun aBlankFileGetsANewId() {
        val store = MemoryTextStore("  \n")

        val id = LocalThisDevice(lazy { store }, canMarkForOthers = false).id

        assertTrue(id.isNotBlank())
        assertEquals(id, store.text)
    }
}
