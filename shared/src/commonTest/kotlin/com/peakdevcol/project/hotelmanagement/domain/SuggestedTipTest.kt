package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.suggestedTip
import com.peakdevcol.project.hotelmanagement.fakes.order
import kotlin.test.Test
import kotlin.test.assertEquals

/** RF-CUR-05: 10 % of the order without taxes, rounded down to the hundred (PRD, section 13, 2026-11-20). */
class SuggestedTipTest {
    @Test
    fun isTenPercentOfTheOrderWithoutTaxes() {
        assertEquals(4_200, suggestedTip(totalAmount = 46_000, taxAmount = 3_407))
    }

    @Test
    fun isRoundedDownSoItNeverGoesOverTheCap() {
        assertEquals(4_600, suggestedTip(totalAmount = 46_000, taxAmount = 0))
        assertEquals(4_600, suggestedTip(totalAmount = 46_999, taxAmount = 0))
        assertEquals(0, suggestedTip(totalAmount = 900, taxAmount = 0))
    }

    @Test
    fun isNeverNegative() {
        assertEquals(0, suggestedTip(totalAmount = 0, taxAmount = 100))
    }

    @Test
    fun anOrderUsesItsOwnTotalAndTaxes() {
        // 19 % VAT included: $11.900 is $10.000 plus $1.900 of tax.
        val withTax = order("o", products = listOf(Product(id = "1", name = "Almuerzo", unitPrice = 11_900, quantity = 2, taxRate = 1_900)))

        assertEquals(2_000, withTax.suggestedTip)
    }
}
