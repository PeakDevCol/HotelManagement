package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.formatTaxRate
import com.peakdevcol.project.hotelmanagement.domain.model.parseTaxRate
import com.peakdevcol.project.hotelmanagement.domain.model.taxAmount
import com.peakdevcol.project.hotelmanagement.domain.model.taxIncludedIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaxTest {
    @Test
    fun theTaxIsTakenFromInsideThePrice() {
        assertEquals(1_900, taxIncludedIn(amount = 11_900, rate = 19_00))
        assertEquals(1_000, taxIncludedIn(amount = 13_500, rate = 8_00))
    }

    @Test
    fun theTaxIsRoundedToTheNearestPeso() {
        // 10.000 with 19 %: 1.596,64 of tax.
        assertEquals(1_597, taxIncludedIn(amount = 10_000, rate = 19_00))
        // 3.500 with 8 %: 259,26 of tax.
        assertEquals(259, taxIncludedIn(amount = 3_500, rate = 8_00))
        // 1.085 with 8,5 %: exactly 85 of tax.
        assertEquals(85, taxIncludedIn(amount = 1_085, rate = 8_50))
    }

    @Test
    fun withoutRateOrAmountThereIsNoTax() {
        assertEquals(0, taxIncludedIn(amount = 11_900, rate = 0))
        assertEquals(0, taxIncludedIn(amount = 0, rate = 19_00))
    }

    @Test
    fun theTaxOfALineComesFromItsTotalAndAnOrderAddsItsLines() {
        val beer = Product(id = "1", name = "Cerveza", unitPrice = 11_900, quantity = 3, taxRate = 19_00)
        val water = Product(id = "5", name = "Agua", unitPrice = 2_000, quantity = 2)

        assertEquals(5_700, beer.taxAmount)
        assertEquals(0, water.taxAmount)
        assertEquals(5_700, listOf(beer, water).taxAmount())
    }

    @Test
    fun readsPercentagesWithCommaOrPoint() {
        assertEquals(19_00, parseTaxRate("19"))
        assertEquals(8_50, parseTaxRate("8,5"))
        assertEquals(8_50, parseTaxRate(" 8.50 "))
        assertEquals(5, parseTaxRate("0,05"))
        assertEquals(100_00, parseTaxRate("100"))
    }

    @Test
    fun rejectsWhatIsNotAPercentageBetweenZeroAndOneHundred() {
        listOf("", "0", "0,00", "100,01", "abc", "19,555", "-5", "1.2.3", ",5", "19%").forEach { text ->
            assertNull(parseTaxRate(text), text)
        }
    }

    @Test
    fun showsThePercentageTheColombianWay() {
        assertEquals("19 %", formatTaxRate(19_00))
        assertEquals("8,5 %", formatTaxRate(8_50))
        assertEquals("8,25 %", formatTaxRate(8_25))
        assertEquals("0,05 %", formatTaxRate(5))
    }
}
