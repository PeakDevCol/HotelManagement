package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.backup.DATED_COLLECTIONS
import com.peakdevcol.project.hotelmanagement.data.backup.DAY_COLLECTIONS
import com.peakdevcol.project.hotelmanagement.data.backup.WHOLE_COLLECTIONS
import com.peakdevcol.project.hotelmanagement.data.firebase.FirestorePaths
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

/** RF-EMP-07: the backup exports every collection, also the ones each milestone adds. */
class BackupCollectionsTest {
    @Test
    fun everyCollectionOfTheAppIsInTheBackup() {
        val collections = FirestorePaths::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.get(null) as String }
            .toSet()
        // The history of each order and of each reservation goes inside it.
        val expected = collections - FirestorePaths.ORDER_EVENTS

        assertEquals(expected, WHOLE_COLLECTIONS.toSet() + DATED_COLLECTIONS.keys + DAY_COLLECTIONS.keys + FirestorePaths.ORDERS + FirestorePaths.RESERVATIONS)
    }
}
