package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.guestSearchKeyFor
import com.peakdevcol.project.hotelmanagement.domain.model.matches
import com.peakdevcol.project.hotelmanagement.domain.model.searchable
import com.peakdevcol.project.hotelmanagement.domain.repository.GuestRepository

/**
 * RF-RES-01: the guests whose name has words that start with what was typed, or whose document is
 * the one typed. Firestore is asked for one key and at most [SEARCH_LIMIT] guests (RNF-12); the
 * rest of the words narrow them on the device.
 */
class SearchGuestsUseCase(
    private val guestRepository: GuestRepository,
) {
    /** By name. Nothing is searched until a word has two letters. */
    suspend operator fun invoke(query: String): Result<List<Guest>> {
        val key = guestSearchKeyFor(query) ?: return Result.success(emptyList())
        return guestRepository.search(key, SEARCH_LIMIT)
            .map { guests -> guests.filter { it.matches(query) }.sortedBy { it.fullName.searchable() } }
    }

    companion object {
        const val SEARCH_LIMIT = 20
    }
}
