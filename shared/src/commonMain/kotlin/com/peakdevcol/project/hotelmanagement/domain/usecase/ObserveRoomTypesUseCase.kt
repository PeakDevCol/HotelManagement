package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.RoomTypeCatalog
import com.peakdevcol.project.hotelmanagement.domain.repository.RoomTypeRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.TaxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** RF-HAB-01: every type of room, active or not, and the taxes of the inventory its rate can include. */
class ObserveRoomTypesUseCase(
    private val roomTypeRepository: RoomTypeRepository,
    private val taxRepository: TaxRepository,
) {
    operator fun invoke(): Flow<RoomTypeCatalog> =
        combine(roomTypeRepository.observeRoomTypes(), taxRepository.observeTaxes()) { types, taxes ->
            RoomTypeCatalog(types = types.sortedBy { it.name.lowercase() }, taxes = taxes.sortedBy { it.name.lowercase() })
        }
}
