package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.SettingsDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toSettings
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.SettingsError
import com.peakdevcol.project.hotelmanagement.domain.repository.SettingsRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

internal class FirestoreSettingsRepository(
    private val firestore: Lazy<FirebaseFirestore>,
) : SettingsRepository {

    private val hotelSettings get() = firestore.value.collection(FirestorePaths.SETTINGS).document(HOTEL_DOCUMENT)

    override fun observeSettings(): Flow<HotelSettings> =
        hotelSettings.snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<SettingsDto>(ServerTimestampBehavior.ESTIMATE).toSettings() else HotelSettings()
        }

    // The parameters change how money is grouped by day, so a change must reach the server:
    // one that only stays on a device would make that device count differently from the rest.
    override suspend fun save(settings: HotelSettings, updatedBy: String): Result<Unit> = try {
        withTimeoutOrNull(SERVER_WAIT) { hotelSettings.set(settings.toDto(updatedBy)) }
            ?.let { Result.success(Unit) }
            ?: Result.failure(SettingsError.Network)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val denied = (e as? FirebaseFirestoreException)?.code == FirestoreExceptionCode.PERMISSION_DENIED
        Result.failure(if (denied) SettingsError.NotAllowed else SettingsError.Unknown)
    }

    private companion object {
        const val HOTEL_DOCUMENT = "hotel"
        val SERVER_WAIT = 10.seconds
    }
}
