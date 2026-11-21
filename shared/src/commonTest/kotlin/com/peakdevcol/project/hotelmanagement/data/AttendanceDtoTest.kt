package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.AttendanceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.CorrectionDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.DeviceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDevice
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRecord
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRegistrationDto
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionKind
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkKind
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.adminMark
import com.peakdevcol.project.hotelmanagement.fakes.phoneMark
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    @Test
    fun theEntryOfAnEmployeeLeavesTheCorrectionsOutAsTheRulesRequire() {
        val encoder = FieldNamesEncoder()
        AttendanceDto.serializer().serialize(encoder, entry.toDto())

        assertFalse("corrections" in encoder.names)
        assertFalse("lastCorrectionId" in encoder.names)
        assertTrue("clockOutAt" in encoder.names)
    }

    @Test
    fun theCorrectionsReadBackTheOldestFirstAndThePendingOneLast() {
        val stored = entry.toDto().copy(
            clockInAt = Timestamp(1_795_183_000, 0),
            corrections = mapOf(
                "c-3" to CorrectionDto(kind = "other", mark = "clockIn", time = Timestamp(1_795_175_000, 0), reason = "Llegó antes", by = "admin", at = Timestamp.ServerTimestamp),
                "c-1" to CorrectionDto(kind = "server", mark = "clockIn", time = Timestamp(1_795_183_000, 0), by = "admin", at = Timestamp(1_795_200_000, 0)),
                "c-2" to CorrectionDto(kind = "device", mark = "clockOut", time = Timestamp(1_795_205_000, 0), by = "admin", at = Timestamp(1_795_210_000, 0)),
                "c-4" to CorrectionDto(kind = "magic", mark = "clockIn", by = "admin"),
            ),
            lastCorrectionId = "c-3",
        )

        val corrections = stored.toRecord("a-1")!!.corrections

        assertEquals(listOf("c-1", "c-2", "c-3"), corrections.map { it.id })
        assertEquals(listOf(CorrectionKind.SERVER_TIME, CorrectionKind.DEVICE_TIME, CorrectionKind.OTHER_TIME), corrections.map { it.kind })
        assertEquals(listOf(MarkKind.ENTRY, MarkKind.EXIT, MarkKind.ENTRY), corrections.map { it.mark })
        assertNull(corrections.last().at)
        assertEquals(Instant.fromEpochSeconds(1_795_175_000), corrections.last().time)
    }

    @Test
    fun aCorrectionAsksForTheTimeOfTheServer() {
        val correction = Correction("c-1", CorrectionKind.OTHER_TIME, MarkKind.EXIT, Instant.fromEpochSeconds(1_795_205_000), "Olvidó marcar", "admin", at = null)

        assertEquals(
            CorrectionDto(kind = "other", mark = "clockOut", time = Timestamp(1_795_205_000, 0), reason = "Olvidó marcar", by = "admin", at = Timestamp.ServerTimestamp),
            correction.toDto(),
        )
    }

    @Test
    fun aRegistrationKeepsTheTimesOfTheAdministratorAsBothTimesOfEachMark() {
        val registration = Correction("c-1", CorrectionKind.REGISTRATION, mark = null, time = null, reason = "Marcó en papel", by = "admin", at = null)
        val record = AttendanceRecord(
            id = "a-9",
            employeeId = "ana",
            clockIn = adminMark(Instant.fromEpochSeconds(1_795_175_880)),
            clockOut = adminMark(Instant.fromEpochSeconds(1_795_204_680)),
            corrections = listOf(registration),
        )

        val dto = record.toRegistrationDto()

        assertEquals(Timestamp(1_795_175_880, 0), dto.clockInAt)
        assertEquals(dto.clockInAt, dto.clockInDeviceTime)
        assertEquals(Timestamp(1_795_204_680, 0), dto.clockOutAt)
        assertEquals("admin" to "admin", dto.clockInSource to dto.clockOutSource)
        assertEquals("c-1", dto.lastCorrectionId)
        assertEquals(CorrectionDto(kind = "registration", reason = "Marcó en papel", by = "admin"), dto.corrections.getValue("c-1"))
        assertEquals(record.copy(corrections = listOf(registration.copy(at = null))), dto.copy(corrections = dto.corrections).toRecord("a-9"))
    }
}

/**
 * Writes nothing: it only notes which fields a serializer sends, as GitLive does when it sends the
 * default values, so the test does not need Firebase.
 */
@OptIn(ExperimentalSerializationApi::class)
private class FieldNamesEncoder : AbstractEncoder() {
    val names = mutableListOf<String>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        names += descriptor.getElementName(index)
        return true
    }

    override fun encodeValue(value: Any) = Unit

    override fun encodeNull() = Unit

    // The values themselves are not needed, and the timestamps only encode inside Firebase.
    override fun <T> encodeSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        encodeElement(descriptor, index)
    }

    override fun <T : Any> encodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        encodeElement(descriptor, index)
    }
}
