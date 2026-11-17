package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.parseTaxRate
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository

/**
 * RF-INV-07: creates or edits a tax, also to deactivate it. Orders keep the rate they copied,
 * so editing a tax only changes the orders taken from then on.
 */
class SaveTaxUseCase(
    private val taxRepository: TaxRepository,
) {
    /** A null [taxId] creates the tax. [rateText] is the percentage as typed, like "19" or "8,5". */
    suspend operator fun invoke(taxId: String?, name: String, rateText: String, active: Boolean = true): Result<Unit> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Result.failure(InventoryError.BlankName)
        val rate = parseTaxRate(rateText) ?: return Result.failure(InventoryError.InvalidTaxRate)
        return taxRepository.save(Tax(id = taxId ?: newId(), name = cleanName, rate = rate, active = active))
    }
}
