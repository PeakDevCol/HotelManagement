package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.AttendanceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.DeviceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDevice
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRecord
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** RF-ASI-03 to RF-ASI-05 and RF-ASI-11. */
class AttendanceDtoTest {
    private val entry = AttendanceRecord(id = "a-1", employeeId = "ana", clockIn = phoneMark(at = null, deviceTime = "2026-11-20T11:58:00Z"))

    @Test
    fun aNewEntryAsksForTheTimeOfTheServerAndKeepsNoCoordinates() {
        val dto = entry.toDto()

        assertEquals(Timestamp.ServerTimestamp, dto.clockInAt)
        assertEquals(Timestamp(1_795_175_880, 0), dto.clockInDeviceTime)
        assertEquals("phone", dto.clockInSource)
        assertEquals(10 to 12, dto.clockInDistanceMeters to dto.clockInAccuracyMeters)
        assertNull(dto.clockOutAt)
        assertNull(dto.clockOutDeviceTime)
    }

    @Test
    fun anEntryThatHasNotReachedTheServerReadsWithoutItsTime() {
        val record = entry.toDto().copy(clockInAt = null).toRecord("a-1")!!

        assertNull(record.clockIn.at)
        assertEquals(Instant.parse("2026-11-20T11:58:00Z"), record.clockIn.deviceTime)
        assertNull(record.clockOut)
    }

    @Test
    fun anExitAtReceptionReadsBackWithTheComputer() {
        val stored = AttendanceDto(
            employeeId = "ana",
            clockInAt = Timestamp(1_795_175_880, 0),
            clockInDeviceTime = Timestamp(1_795_175_880, 0),
            clockInSource = "phone",
            clockOutAt = Timestamp(1_795_205_000, 0),
            clockOutDeviceTime = Timestamp(1_795_205_000, 0),
            clockOutSource = "kiosk",
            clockOutDeviceId = "reception-pc",
        )

        val exit = stored.toRecord("a-1")!!.clockOut!!

        assertEquals(Mark(Instant.fromEpochSeconds(1_795_205_000), Instant.fromEpochSeconds(1_795_205_000), MarkSource.KIOSK, "reception-pc"), exit)
    }

    @Test
    fun anUnknownSourceIsNotRead() {
        assertNull(entry.toDto().copy(clockInSource = "tablet").toRecord("a-1"))
    }

    @Test
    fun anAuthorizedComputerAsksForTheTimeOfTheServer() {
        val dto = AuthorizedDevice(id = "reception-pc", name = "Recepción", authorizedBy = "admin").toDto()

        assertEquals(DeviceDto("Recepción", "admin", Timestamp.ServerTimestamp, active = true), dto)
        assertEquals(
            AuthorizedDevice("reception-pc", "Recepción", "admin", Instant.fromEpochSeconds(1_795_175_880), active = false),
            dto.copy(authorizedAt = Timestamp(1_795_175_880, 0), active = false).toDevice("reception-pc"),
        )
    }
}
