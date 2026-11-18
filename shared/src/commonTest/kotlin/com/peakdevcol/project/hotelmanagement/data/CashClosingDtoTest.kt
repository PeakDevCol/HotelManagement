package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toCashClosing
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.CashClosingType
import com.peakdevcol.project.hotelmanagement.fakes.cashClosing
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CashClosingDtoTest {
    private val closing = cashClosing(
        number = 7,
        from = "2026-11-16T03:00:00.123456Z",
        to = "2026-11-17T02:30:00.654321Z",
        createdAt = "2026-11-17T03:00:00Z",
        type = CashClosingType.MANUAL,
    )

    @Test
    fun typesUseTheCodesOfThePrd() {
        assertEquals(listOf("daily", "manual"), CashClosingType.entries.map { it.code() })
    }

    @Test
    fun aNewClosingIsStoredWithTheServerDate() {
        val dto = closing.toDto()

        assertEquals(Timestamp.ServerTimestamp, dto.createdAt)
        assertEquals("manual", dto.type)
    }

    // The rules compare the start of a closing with the end of the one before, so the moments
    // must come back exactly as they were stored, microseconds included.
    @Test
    fun keepsTheExactMomentsOfThePeriod() {
        val stored = closing.toDto().copy(createdAt = Timestamp(closing.createdAt!!.epochSeconds, 0))

        val read = stored.toCashClosing("7")!!

        assertEquals(closing.from to closing.to, read.from to read.to)
        assertEquals(closing.createdAt, read.createdAt)
        assertEquals(7, read.number)
    }

    @Test
    fun anIdThatIsNotANumberIsLeftOut() {
        assertNull(closing.toDto().toCashClosing("abc"))
    }
}
