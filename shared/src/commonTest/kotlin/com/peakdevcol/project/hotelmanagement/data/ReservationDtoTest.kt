package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.ReservationDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.RoomNightDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toGuest
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toNewDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toReservation
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toRoomNight
import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReservationState
import com.peakdevcol.project.hotelmanagement.fakes.day
import com.peakdevcol.project.hotelmanagement.fakes.reservation
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The fields and values of section 8 of the PRD, which the rules check (M11). */
class ReservationDtoTest {
    @Test
    fun aNewGuestIsStoredWithItsSearchKeysSignedAndWithoutAuthorizationYet() {
        val guest = Guest(id = "g", fullName = "Ana Rojas", phone = "3001234567", documentType = DocumentType.PASSPORT, documentNumber = "AB123")

        val dto = guest.toNewDto(createdBy = "reception")

        assertEquals("passport", dto.documentType)
        assertEquals("reception", dto.createdBy)
        assertEquals(Timestamp.ServerTimestamp, dto.createdAt)
        assertNull(dto.dataConsentAt)
        assertTrue("roj" in dto.searchKeys && "ab123" in dto.searchKeys)
        assertEquals(guest, dto.toGuest("g"))
    }

    @Test
    fun aNewReservationIsStoredConfirmedWithItsDaysAsTextAndItsTotal() {
        val made = reservation("r1", roomId = "r-204", checkIn = "2026-11-30", checkOut = "2026-12-02", rate = 250_000).copy(createdBy = "reception")

        val dto = made.toNewDto()

        assertEquals("2026-11-30", dto.checkInDate)
        assertEquals("2026-12-02", dto.checkOutDate)
        assertEquals(listOf(250_000L, 250_000L), dto.nightlyRates)
        assertEquals(500_000, dto.total)
        assertEquals(0, dto.paidAmount)
        assertEquals("phone", dto.source)
        assertEquals("confirmed", dto.state)
        assertEquals(Timestamp.ServerTimestamp, dto.createdAt)
        assertEquals(made, dto.copy(createdAt = null).toReservation("r1"))
    }

    @Test
    fun theCodesOfSectionEightAreReadBack() {
        val dto = ReservationDto(checkInDate = "2026-11-30", checkOutDate = "2026-12-01", source = "whatsapp", state = "checked_in")

        val read = dto.toReservation("r1")!!

        assertEquals(ReservationSource.WHATSAPP, read.source)
        assertEquals(ReservationState.CHECKED_IN, read.state)
        assertEquals(ReservationState.NO_SHOW, dto.copy(state = "no_show").toReservation("r1")!!.state)
        assertNull(dto.copy(checkInDate = "30/11/2026").toReservation("r1"), "wrong days never become wrong nights")
    }

    @Test
    fun aNightIsStoredWithItsRoomItsDayAndItsReservation() {
        val night = reservation("r1", roomId = "r-204", checkIn = "2026-11-30", checkOut = "2026-12-01").roomNights.single()

        assertEquals(RoomNightDto(roomId = "r-204", date = "2026-11-30", reservationId = "r1", arrival = true), night.toDto())
        assertEquals(night, night.toDto().toRoomNight())
        assertEquals(day("2026-11-30"), night.date)
    }
}
