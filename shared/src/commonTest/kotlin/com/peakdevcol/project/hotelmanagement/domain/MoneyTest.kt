package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {
    @Test
    fun groupsThousandsWithDots() {
        assertEquals("$0", formatPesos(0))
        assertEquals("$999", formatPesos(999))
        assertEquals("$6.000", formatPesos(6_000))
        assertEquals("$1.234.567", formatPesos(1_234_567))
    }

    @Test
    fun putsTheSignBeforeTheCurrencySymbol() {
        assertEquals("-$1.500", formatPesos(-1_500))
    }
}
