package com.peakdevcol.project.hotelmanagement.data

import com.peakdevcol.project.hotelmanagement.data.backup.JsonWriter
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.SettingsDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toSettings
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class JsonWriterTest {
    @Test
    fun writesTheBasicValues() {
        assertEquals("null", JsonWriter.write(null))
        assertEquals("true", JsonWriter.write(true))
        assertEquals("24500", JsonWriter.write(24_500L))
        assertEquals("\"Hotel\"", JsonWriter.write("Hotel"))
        assertEquals("{}", JsonWriter.write(emptyMap<String, Any?>()))
        assertEquals("[]", JsonWriter.write(emptyList<Any?>()))
    }

    @Test
    fun escapesWhatWouldBreakTheText() {
        assertEquals("\"Mesa \\\"3\\\" \\\\ bar\\nlínea\\ttab\"", JsonWriter.write("Mesa \"3\" \\ bar\nlínea\ttab"))
        assertEquals("\"\\u0001\"", JsonWriter.write("\u0001"))
    }

    @Test
    fun writesNestedDocumentsWithTheKeysInOrder() {
        val order = mapOf(
            "state" to "paid",
            "products" to listOf(mapOf("quantity" to 2, "productName" to "Cerveza")),
            "cancelReason" to null,
        )

        val expected = """
            {
              "cancelReason": null,
              "products": [
                {
                  "productName": "Cerveza",
                  "quantity": 2
                }
              ],
              "state": "paid"
            }
        """.trimIndent()
        assertEquals(expected, JsonWriter.write(order))
    }

    @Test
    fun aNumberThatIsNotANumberBecomesNull() {
        assertEquals("null", JsonWriter.write(Double.NaN))
        assertEquals("1.5", JsonWriter.write(1.5))
    }

    @Test
    fun theSettingsAreStoredWithWhoChangedThemAndTheServerDate() {
        val stored = HotelSettings(operationalDayStartMinutes = 330).toDto(updatedBy = "admin")

        assertEquals(330 to "admin", stored.operationalDayStartMinutes to stored.updatedBy)
        assertEquals(Timestamp.ServerTimestamp, stored.updatedAt)
    }

    @Test
    fun readsBackTheStoredSettingsAndFallsBackToTheDefaults() {
        val stored = SettingsDto(operationalDayStartMinutes = 330, updatedBy = "admin", updatedAt = Timestamp(1_794_702_180, 0))

        assertEquals(
            HotelSettings(330, updatedBy = "admin", updatedAt = Instant.fromEpochSeconds(1_794_702_180)),
            stored.toSettings(),
        )
        assertEquals(HotelSettings(), SettingsDto().toSettings())
    }
}
