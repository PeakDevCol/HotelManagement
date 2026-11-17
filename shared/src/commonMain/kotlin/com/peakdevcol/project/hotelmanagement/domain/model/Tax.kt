package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * An optional tax of the inventory, like IVA or the consumption tax (RF-INV-07, P14).
 * A product has one or none, and its sale price already includes it.
 */
data class Tax(
    val id: String,
    val name: String,
    /** The percentage in hundredths of a point, so 19 % is 1900 and 8,5 % is 850. Never a decimal number. */
    val rate: Int,
    /** An inactive tax is kept, but new orders no longer apply it. */
    val active: Boolean = true,
)

const val MAX_TAX_RATE = 100_00

/**
 * How much of an amount that already includes the tax is tax, rounded to the nearest peso.
 * With 19 %, a price of $11.900 holds $1.900 of tax over a base of $10.000.
 */
fun taxIncludedIn(amount: Long, rate: Int): Long {
    if (rate <= 0 || amount <= 0) return 0
    val divisor = MAX_TAX_RATE + rate
    return (amount * rate + divisor / 2) / divisor
}

/** Reads a percentage typed with a comma or a point and up to two decimals. Null when it is not a valid one. */
fun parseTaxRate(text: String): Int? {
    val parts = text.trim().replace(',', '.').split('.')
    if (parts.size > 2) return null
    val whole = parts[0].toIntOrNull()?.takeIf { parts[0].all(Char::isDigit) } ?: return null
    val decimals = parts.getOrElse(1) { "" }
    if (decimals.length > 2 || !decimals.all(Char::isDigit)) return null
    val rate = whole * 100 + decimals.padEnd(2, '0').toInt()
    return rate.takeIf { it in 1..MAX_TAX_RATE }
}

/** Formats a rate the way it is typed in Colombia: `19 %`, `8,5 %`. */
fun formatTaxRate(rate: Int): String {
    val decimals = (rate % 100).toString().padStart(2, '0').trimEnd('0')
    return if (decimals.isEmpty()) "${rate / 100} %" else "${rate / 100},$decimals %"
}
