package com.peakdevcol.project.hotelmanagement.ui.inventory

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_blank_name
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_category_in_use
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_duplicate_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_missing_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_not_allowed
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_not_found
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_price
import com.peakdevcol.project.hotelmanagement.resources.inventory_error_unknown
import org.jetbrains.compose.resources.StringResource

fun InventoryError.message(): StringResource = when (this) {
    InventoryError.BlankName -> Res.string.inventory_error_blank_name
    InventoryError.InvalidPrice -> Res.string.inventory_error_price
    InventoryError.MissingCategory -> Res.string.inventory_error_missing_category
    InventoryError.DuplicateCategory -> Res.string.inventory_error_duplicate_category
    InventoryError.CategoryInUse -> Res.string.inventory_error_category_in_use
    InventoryError.NotFound -> Res.string.inventory_error_not_found
    InventoryError.NotAllowed -> Res.string.inventory_error_not_allowed
    InventoryError.Unknown -> Res.string.inventory_error_unknown
}
