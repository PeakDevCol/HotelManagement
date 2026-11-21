package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.adminMark
import com.peakdevcol.project.hotelmanagement.domain.model.entryNear
import com.peakdevcol.project.hotelmanagement.domain.model.exitAfter
import com.peakdevcol.project.hotelmanagement.domain.model.forgottenAt
import com.peakdevcol.project.hotelmanagement.domain.model.history
import com.peakdevcol.project.hotelmanagement.domain.model.isPendingExit
import com.peakdevcol.project.hotelmanagement.domain.model.timesError
import com.peakdevcol.project.hotelmanagement.fakes.bogotaInstant
import com.peakdevcol.project.hotelmanagement.fakes.bogotaRecord
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** RF-ASI-05 to RF-ASI-07. */
class AttendanceCorrectionTest {
    private fun bogota(text: String) = Instant.parse(bogotaInstant(text))

    // Marked at 7:02 on a phone without connection; it reached the server at 9:15.
    private val underReview = AttendanceRecord(
        id = "a-1",
        employeeId = "ana",
        clockIn = phoneMark(at = bogotaInstant("2026-11-20 09:15"), deviceTime = bogotaInstant("2026-11-20 07:02")),
        clockOut = phoneMark(at = bogotaInstant("2026-11-20 14:01")),
    )

    private fun correction(kind: CorrectionKind, mark: MarkKind?, time: String?, at: String, id: String = at) = Correction(
        id = id,
        kind = kind,
        mark = mark,
        time = time?.let(::bogota),
        reason = if (kind == CorrectionKind.OTHER_TIME || kind == CorrectionKind.REGISTRATION) "Olvidó marcar" else null,
        by = "admin",
        at = bogota(at),
    )

    @Test
    fun untilTheAdministratorChoosesTheTimeOfTheDeviceCounts() {
        assertTrue(underReview.entryNeedsReview)
        assertEquals(bogota("2026-11-20 07:02"), underReview.entryTime)
    }

    @Test
    fun aCorrectionChangesTheTimeThatCountsAndKeepsTheMarkAsItWas() {
        val corrected = underReview.copy(corrections = listOf(correction(CorrectionKind.SERVER_TIME, MarkKind.ENTRY, "2026-11-20 09:15", at = "2026-11-21 08:00")))

        assertEquals(bogota("2026-11-20 09:15"), corrected.entryTime)
        assertFalse(corrected.needsReview)
        assertEquals(underReview.clockIn, corrected.clockIn)
        assertTrue(corrected.isCorrected)
    }

    @Test
    fun theLastCorrectionOfAMarkIsTheOneThatCounts() {
        val corrected = underReview.copy(
            corrections = listOf(
                correction(CorrectionKind.SERVER_TIME, MarkKind.ENTRY, "2026-11-20 09:15", at = "2026-11-21 08:00"),
                correction(CorrectionKind.OTHER_TIME, MarkKind.ENTRY, "2026-11-20 07:00", at = "2026-11-21 08:05"),
            ),
        )

        assertEquals(bogota("2026-11-20 07:00"), corrected.entryTime)
        assertEquals(bogota("2026-11-20 14:01") - bogota("2026-11-20 07:00"), corrected.workedTime)
    }

    @Test
    fun correctingTheEntryLeavesAnExitUnderReviewWaiting() {
        val bothUnderReview = underReview.copy(clockOut = phoneMark(at = bogotaInstant("2026-11-20 16:00"), deviceTime = bogotaInstant("2026-11-20 14:00")))
        val corrected = bothUnderReview.copy(corrections = listOf(correction(CorrectionKind.DEVICE_TIME, MarkKind.ENTRY, "2026-11-20 07:02", at = "2026-11-21 08:00")))

        assertFalse(corrected.entryNeedsReview)
        assertTrue(corrected.exitNeedsReview)
        assertTrue(corrected.needsReview)
    }

    @Test
    fun theHistoryTellsWhatCountedBeforeEachCorrection() {
        val corrected = underReview.copy(
            corrections = listOf(
                correction(CorrectionKind.SERVER_TIME, MarkKind.ENTRY, "2026-11-20 09:15", at = "2026-11-21 08:00"),
                correction(CorrectionKind.OTHER_TIME, MarkKind.ENTRY, "2026-11-20 07:00", at = "2026-11-21 08:05"),
                correction(CorrectionKind.OTHER_TIME, MarkKind.EXIT, "2026-11-20 14:00", at = "2026-11-21 08:06"),
            ),
        )

        assertEquals(
            listOf(bogota("2026-11-20 07:02"), bogota("2026-11-20 09:15"), bogota("2026-11-20 14:01")),
            corrected.history().map { it.previousTime },
        )
    }

    @Test
    fun anExitWrittenByTheAdministratorHadNoTimeBefore() {
        val forgotten = bogotaRecord("a-2", "ana", "2026-11-20 06:00")
        val closed = forgotten.copy(
            clockOut = adminMark(bogota("2026-11-20 14:00")),
            corrections = listOf(correction(CorrectionKind.OTHER_TIME, MarkKind.EXIT, "2026-11-20 14:00", at = "2026-11-21 08:00")),
        )

        assertNull(closed.history().single().previousTime)
        assertFalse(closed.isOpen)
        assertFalse(closed.needsReview)
        assertEquals(bogota("2026-11-20 14:00") - bogota("2026-11-20 06:00"), closed.workedTime)
    }

    @Test
    fun aRegistrationIsOneStepAndLaterChangesStartFromItsTimes() {
        val registered = AttendanceRecord(
            id = "a-3",
            employeeId = "ana",
            clockIn = adminMark(bogota("2026-11-21 06:00")),
            clockOut = adminMark(bogota("2026-11-21 14:00")),
            corrections = listOf(
                correction(CorrectionKind.REGISTRATION, mark = null, time = null, at = "2026-11-22 08:00"),
                correction(CorrectionKind.OTHER_TIME, MarkKind.EXIT, "2026-11-21 15:00", at = "2026-11-22 08:10"),
            ),
        )

        assertEquals(listOf(null, bogota("2026-11-21 14:00")), registered.history().map { it.previousTime })
        assertEquals(bogota("2026-11-21 15:00"), registered.exitTime)
    }

    @Test
    fun aTypedEntryIsTheMomentNearestToTheOneItReplacesAcrossMidnight() {
        val lateAtNight = bogota("2026-11-20 23:50")

        assertEquals(bogota("2026-11-21 00:10"), entryNear(lateAtNight, minuteOfDay = 10))
        assertEquals(bogota("2026-11-20 23:30"), entryNear(lateAtNight, minuteOfDay = 23 * 60 + 30))
        assertEquals(bogota("2026-11-20 06:45"), entryNear(bogota("2026-11-20 07:02"), minuteOfDay = 6 * 60 + 45))
    }

    @Test
    fun aTypedExitNotAfterTheEntryIsOnTheNextDay() {
        val entry = bogota("2026-11-20 22:00")

        assertEquals(bogota("2026-11-21 06:00"), exitAfter(entry, minuteOfDay = 6 * 60))
        assertEquals(bogota("2026-11-20 23:00"), exitAfter(entry, minuteOfDay = 23 * 60))
        assertEquals(bogota("2026-11-21 22:00"), exitAfter(entry, minuteOfDay = 22 * 60))
    }

    @Test
    fun theTimesMakeAShiftOnlyInOrderAndWithinADay() {
        val entry = bogotaRecord("a-4", "ana", "2026-11-20 06:00")

        assertNull(entry.timesError())
        assertNull(entry.copy(clockOut = adminMark(bogota("2026-11-21 06:00"))).timesError())
        assertEquals(CorrectionError.ExitBeforeEntry, entry.copy(clockOut = adminMark(bogota("2026-11-20 06:00"))).timesError())
        assertEquals(CorrectionError.TooLong, entry.copy(clockOut = adminMark(bogota("2026-11-21 06:01"))).timesError())
    }

    @Test
    fun aForgottenEntryCountsFromItsCorrectedTime() {
        val corrected = bogotaRecord("a-5", "ana", "2026-11-20 09:00")
            .copy(corrections = listOf(correction(CorrectionKind.OTHER_TIME, MarkKind.ENTRY, "2026-11-20 06:00", at = "2026-11-20 10:00")))

        assertEquals(bogota("2026-11-20 22:00"), corrected.forgottenAt(maxOpenEntryHours = 16))
        assertTrue(corrected.isPendingExit(bogota("2026-11-20 22:00"), maxOpenEntryHours = 16))
        assertFalse(corrected.isPendingExit(bogota("2026-11-20 21:59"), maxOpenEntryHours = 16))
    }
}
