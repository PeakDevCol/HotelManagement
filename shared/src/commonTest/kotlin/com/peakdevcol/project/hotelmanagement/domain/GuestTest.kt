package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.DocumentType
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.GuestFields
import com.peakdevcol.project.hotelmanagement.domain.model.guestSearchKeyFor
import com.peakdevcol.project.hotelmanagement.domain.model.matches
import com.peakdevcol.project.hotelmanagement.domain.model.searchKeys
import com.peakdevcol.project.hotelmanagement.domain.model.toGuest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** RF-RES-01 with decision 5A and S9. */
class GuestTest {
    private val ana = Guest(id = "ana", fullName = "Ana María Rojas", phone = "300 123 4567", documentType = DocumentType.CC, documentNumber = "1020304")

    @Test
    fun aGuestIsFoundByTheBeginningOfAnyWordOfTheNameOrByTheWholeDocument() {
        val keys = ana.searchKeys

        assertTrue(listOf("an", "ana", "ma", "mar", "maria", "ro", "rojas", "1020304").all { it in keys })
        assertFalse("a" in keys, "one letter would bring back half of the guests")
        assertFalse("102" in keys, "a document is compared whole")
    }

    @Test
    fun theSearchAsksForTheLongestWordOrForTheDocumentTypedWithDots() {
        assertEquals("rojas", guestSearchKeyFor("ana Rojas"))
        assertEquals("1020304", guestSearchKeyFor("1.020.304"))
        assertNull(guestSearchKeyFor("a"))
        assertNull(guestSearchKeyFor("  "))
    }

    @Test
    fun everyWordTypedMustStartAWordOfTheName() {
        assertTrue(ana.matches("ana roj"))
        assertTrue(ana.matches("MARIA"))
        assertTrue(ana.matches("1.020.304"))
        assertFalse(ana.matches("ana lopez"))
        assertFalse(ana.matches("102"))
    }

    @Test
    fun theNameAndThePhoneAreEnoughAndTheDocumentGoesWithItsType() {
        val minimal = GuestFields(fullName = "  Ana   Rojas ", phone = "300 123 4567").toGuest("g", existing = null).getOrThrow()

        assertEquals("Ana Rojas", minimal.fullName)
        assertNull(minimal.documentType)
        assertEquals("Colombia", minimal.nationality)
        assertEquals(GuestError.BlankName, GuestFields(fullName = " ", phone = "3001234567").toGuest("g", null).exceptionOrNull())
        assertEquals(GuestError.InvalidPhone, GuestFields(fullName = "Ana", phone = "300").toGuest("g", null).exceptionOrNull())
        assertEquals(GuestError.IncompleteDocument, GuestFields("Ana", "3001234567", documentType = DocumentType.CC).toGuest("g", null).exceptionOrNull())
        assertEquals(GuestError.IncompleteDocument, GuestFields("Ana", "3001234567", documentNumber = "123").toGuest("g", null).exceptionOrNull())
        assertEquals(GuestError.InvalidEmail, GuestFields("Ana", "3001234567", email = "ana@").toGuest("g", null).exceptionOrNull())
    }

    @Test
    fun theDocumentIsStoredWithoutDotsAndInCapitals() {
        val guest = GuestFields("Ana", "3001234567", documentType = DocumentType.PASSPORT, documentNumber = "ab-12.345").toGuest("g", null).getOrThrow()

        assertEquals("AB12345", guest.documentNumber)
    }

    @Test
    fun editingKeepsTheAuthorizationTheCheckInRecorded() {
        val consent = Instant.parse("2026-11-20T15:00:00Z")
        val edited = GuestFields("Ana Rojas", "3001234567").toGuest("ana", existing = ana.copy(dataConsentAt = consent)).getOrThrow()

        assertEquals(consent, edited.dataConsentAt)
    }

    @Test
    fun theTextOfAGuestCarriesNoPersonalData() {
        assertEquals("Guest(id=ana)", ana.toString())
        assertFalse("1020304" in GuestFields("Ana", "3001234567", documentNumber = "1020304").toString())
    }
}
