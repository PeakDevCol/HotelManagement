package com.peakdevcol.project.hotelmanagement.domain.model

/**
 * Formats an amount in Colombian pesos the way the hotel writes it: `$6.000`, `-$1.500`.
 * It is written by hand because number formatting is not available in common code.
 */
fun formatPesos(amount: Long): String {
    val digits = if (amount < 0) (-amount).toString() else amount.toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (amount < 0) "-$$grouped" else "$$grouped"
}
