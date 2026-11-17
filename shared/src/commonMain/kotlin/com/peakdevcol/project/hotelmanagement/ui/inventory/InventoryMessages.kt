package com.peakdevcol.project.hotelmanagement.ui.inventory

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.formatTaxRate
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_blank_name
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_category_in_use
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_duplicate_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_missing_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_not_found
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_price
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_tax_rate
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_unknown
import com.peakdevcol.project.hotelmanagement.resources.inventory_tax_inactive_label
import com.peakdevcol.project.hotelmanagement.resources.inventory_tax_label
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun InventoryError.message(): StringResource = when (this) {
    InventoryError.BlankName -> Res.string.inventory_error_blank_name
    InventoryError.InvalidPrice -> Res.string.inventory_error_price
    InventoryError.MissingCategory -> Res.string.inventory_error_missing_category
    InventoryError.DuplicateCategory -> Res.string.inventory_error_duplicate_category
    InventoryError.InvalidTaxRate -> Res.string.inventory_error_tax_rate
    InventoryError.CategoryInUse -> Res.string.inventory_error_category_in_use
    InventoryError.NotFound -> Res.string.inventory_error_not_found
    InventoryError.NotAllowed -> Res.string.inventory_error_not_allowed
    InventoryError.Unknown -> Res.string.inventory_error_unknown
}

/** A tax as the lists show it: `IVA (19 %)`. */
@Composable
fun Tax.label(): String {
    val format = if (active) Res.string.inventory_tax_label else Res.string.inventory_tax_inactive_label
    return stringResource(format, name, formatTaxRate(rate))
}
