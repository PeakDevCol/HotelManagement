package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.GuestFields
import com.peakdevcol.project.hotelmanagement.domain.model.newId
import com.peakdevcol.project.hotelmanagement.domain.model.searchable
import com.peakdevcol.project.hotelmanagement.domain.model.toGuest
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.GuestRepository

/**
 * RF-RES-01: creates a guest, or completes the data of one, like the document that was not given
 * when reserving (decision 5A). A document is not repeated: the app checks it, as with the numbers
 * of the rooms, and the ids carry no personal data (S9).
 */
class SaveGuestUseCase(
    private val authRepository: AuthRepository,
    private val guestRepository: GuestRepository,
) {
    /** A null [existing] creates the guest. Returns the guest as it was stored. */
    suspend operator fun invoke(existing: Guest?, fields: GuestFields): Result<Guest> {
        val userId = authRepository.currentUserId() ?: return Result.failure(GuestError.NoSession)
        val guest = fields.toGuest(id = existing?.id ?: newId(), existing = existing).getOrElse { return Result.failure(it) }
        if (hasDocumentOfAnother(guest).getOrElse { return Result.failure(it) }) return Result.failure(GuestError.DuplicateDocument)
        val saved = if (existing == null) guestRepository.create(guest, createdBy = userId) else guestRepository.update(guest, updatedBy = userId)
        return saved.map { guest }
    }

    private suspend fun hasDocumentOfAnother(guest: Guest): Result<Boolean> {
        val number = guest.documentNumber ?: return Result.success(false)
        return guestRepository.search(number.searchable(), SearchGuestsUseCase.SEARCH_LIMIT).map { found ->
            found.any { other -> other.id != guest.id && other.documentType == guest.documentType && other.documentNumber == number }
        }
    }
}
