package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.effectiveTime
import com.peakdevcol.project.hotelmanagement.domain.model.forgottenAt
import com.peakdevcol.project.hotelmanagement.domain.model.needsReview
import com.peakdevcol.project.hotelmanagement.domain.model.toClose
import com.peakdevcol.project.hotelmanagement.fakes.entry
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** RF-ASI-03 to RF-ASI-06. */
class AttendanceTest {
    @Test
    fun aMarkThatHasNotReachedTheServerCountsWithTheTimeOfThePhone() {
        val mark = phoneMark(at = null, deviceTime = "2026-11-20T11:58:00Z")

        assertFalse(mark.needsReview)
        assertEquals(Instant.parse("2026-11-20T11:58:00Z"), mark.effectiveTime)
    }

    @Test
    fun withinFiveMinutesTheTimeOfTheServerCounts() {
        val mark = phoneMark(at = "2026-11-20T12:03:00Z", deviceTime = "2026-11-20T11:58:00Z")

        assertFalse(mark.needsReview)
        assertEquals(Instant.parse("2026-11-20T12:03:00Z"), mark.effectiveTime)
    }

    @Test
    fun moreThanFiveMinutesApartTheMarkWaitsForReviewWithTheTimeOfThePhone() {
        val mark = phoneMark(at = "2026-11-20T12:03:01Z", deviceTime = "2026-11-20T11:58:00Z")

        assertTrue(mark.needsReview)
        assertEquals(Instant.parse("2026-11-20T11:58:00Z"), mark.effectiveTime)
    }

    @Test
    fun theHoursWorkedGoFromTheEntryToTheExit() {
        val record = entry("a-1", "ana", at = "2026-11-20T11:00:00Z", clockOut = "2026-11-20T19:05:00Z")

        assertEquals(8.hours + 5.minutes, record.workedTime)
        assertFalse(record.isOpen)
    }

    @Test
    fun anOpenEntryIsTheOneToCloseUntilTheHoursOfTheParameterPass() {
        val record = entry("a-1", "ana", at = "2026-11-20T11:00:00Z")

        assertEquals(Instant.parse("2026-11-21T03:00:00Z"), record.forgottenAt(maxOpenEntryHours = 16))
        assertEquals(record, record.toClose(Instant.parse("2026-11-21T02:59:00Z"), maxOpenEntryHours = 16))
        assertNull(record.toClose(Instant.parse("2026-11-21T03:00:00Z"), maxOpenEntryHours = 16))
    }
}
