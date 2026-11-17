package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val INVENTORY_SERVER_WAIT: Duration = 3.seconds

/**
 * Runs a write to the inventory. Firestore applies it on the device at once, so the lists show
 * it right away, and sends it when there is connection (RNF-04). The wait is only there to hear
 * a rejection from the server, like the one a non-administrator gets from the rules.
 */
internal suspend fun inventoryWrite(serverWait: Duration, write: suspend () -> Unit): Result<Unit> = try {
    withTimeoutOrNull(serverWait) { write() }
    Result.success(Unit)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    val denied = (e as? FirebaseFirestoreException)?.code == FirestoreExceptionCode.PERMISSION_DENIED
    Result.failure(if (denied) InventoryError.NotAllowed else InventoryError.Unknown)
}
