package com.peakdevcol.project.hotelmanagement.domain.model

private const val SUGGESTED_TIP_PERCENT = 10
private const val TIP_ROUNDING = 100L

/**
 * RF-CUR-05: the tip offered when charging. It is 10 % of the order without taxes, rounded down to
 * the hundred (PRD, section 13, 2026-11-20). The law caps a suggested tip at 10 % of the service
 * (Ley 1935 de 2018) without saying whether taxes count, so this never goes over it.
 */
fun suggestedTip(totalAmount: Long, taxAmount: Long): Long {
    val withoutTaxes = (totalAmount - taxAmount).coerceAtLeast(0)
    val tip = withoutTaxes * SUGGESTED_TIP_PERCENT / 100
    return tip / TIP_ROUNDING * TIP_ROUNDING
}

/** RF-CUR-05: the tip offered for this order. */
val Order.suggestedTip: Long get() = suggestedTip(totalAmount, taxAmount)
