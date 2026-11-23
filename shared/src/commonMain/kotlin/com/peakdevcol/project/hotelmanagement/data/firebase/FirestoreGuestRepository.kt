package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.GuestDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.code
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toGuest
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toNewDto
import com.peakdevcol.project.hotelmanagement.domain.model.Guest
import com.peakdevcol.project.hotelmanagement.domain.model.GuestError
import com.peakdevcol.project.hotelmanagement.domain.model.searchKeys
import com.peakdevcol.project.hotelmanagement.domain.repository.GuestRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class FirestoreGuestRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = RESERVATION_SERVER_WAIT,
) : GuestRepository {

    private val guests get() = firestore.value.collection(FirestorePaths.GUESTS)

    // Source.SERVER: a guest missing from the cache of the device would let a document be repeated.
    // Guests are looked up to reserve, which needs connection anyway (RNF-04).
    override suspend fun search(key: String, limit: Int): Result<List<Guest>> = guarded {
        withTimeoutOrNull(serverWait) { guests.where { GUEST_SEARCH_KEYS_FIELD contains key }.limit(limit).get(Source.SERVER) }
            ?.let { snapshot -> Result.success(snapshot.documents.map { it.data<GuestDto>(ServerTimestampBehavior.ESTIMATE).toGuest(it.id) }) }
            ?: Result.failure(GuestError.NoConnection)
    }

    override suspend fun guest(guestId: String): Result<Guest?> = guarded {
        withTimeoutOrNull(serverWait) { guests.document(guestId).get(Source.SERVER) }
            ?.let { snapshot -> Result.success(if (snapshot.exists) snapshot.data<GuestDto>(ServerTimestampBehavior.ESTIMATE).toGuest(snapshot.id) else null) }
            ?: Result.failure(GuestError.NoConnection)
    }

    override suspend fun create(guest: Guest, createdBy: String): Result<Unit> = guarded {
        withTimeoutOrNull(serverWait) { guests.document(guest.id).set(guest.toNewDto(createdBy)) }
        Result.success(Unit)
    }

    // Only what the reception types: the authorization of the data belongs to the check-in (M12).
    override suspend fun update(guest: Guest, updatedBy: String): Result<Unit> = guarded {
        withTimeoutOrNull(serverWait) {
            guests.document(guest.id).updateFields {
                GUEST_FULL_NAME_FIELD to guest.fullName
                GUEST_PHONE_FIELD to guest.phone
                GUEST_DOCUMENT_TYPE_FIELD to guest.documentType?.code()
                GUEST_DOCUMENT_NUMBER_FIELD to guest.documentNumber
                GUEST_NATIONALITY_FIELD to guest.nationality
                GUEST_EMAIL_FIELD to guest.email
                GUEST_SEARCH_KEYS_FIELD to guest.searchKeys
                GUEST_UPDATED_BY_FIELD to updatedBy
                GUEST_UPDATED_AT_FIELD to FieldValue.serverTimestamp
            }
        }
        Result.success(Unit)
    }

    private inline fun <T> guarded(block: () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toGuestError())
    }
}
