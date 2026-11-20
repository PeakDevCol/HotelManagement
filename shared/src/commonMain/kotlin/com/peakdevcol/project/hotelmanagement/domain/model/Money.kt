package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * Formats an amount in Colombian pesos the way the hotel writes it: `$6.000`, `-$1.500`.
 * It is written by hand because number formatting is not available in common code.
 */
fun formatPesos(amount: Long): String {
    val grouped = formatThousands(if (amount < 0) -amount else amount)
    return if (amount < 0) "-$$grouped" else "$$grouped"
}

/** A whole number with a dot every three digits, as in Colombia: `1.850`. */
fun formatThousands(number: Long): String {
    val digits = if (number < 0) (-number).toString() else number.toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (number < 0) "-$grouped" else grouped
}
