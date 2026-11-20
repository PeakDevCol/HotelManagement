package com.peakdevcol.project.hotelmanagement.data.firebase

import com.peakdevcol.project.hotelmanagement.data.firebase.dto.DeviceDto
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDevice
import com.peakdevcol.project.hotelmanagement.data.firebase.dto.toDto
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FirestoreDeviceRepository(
    private val firestore: Lazy<FirebaseFirestore>,
    private val serverWait: Duration = 3.seconds,
) : DeviceRepository {

    private val devices get() = firestore.value.collection(FirestorePaths.DEVICES)

    // A handful of computers: the whole collection is listened to.
    override fun observeDevices(): Flow<List<AuthorizedDevice>> =
        devices.snapshots.map { snapshot -> snapshot.documents.map { it.data<DeviceDto>(ServerTimestampBehavior.ESTIMATE).toDevice(it.id) } }

    // The rules let any employee read a computer by its id, but only an administrator list them.
    override fun observeDevice(deviceId: String): Flow<AuthorizedDevice?> =
        devices.document(deviceId).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<DeviceDto>(ServerTimestampBehavior.ESTIMATE).toDevice(snapshot.id) else null
        }

    override suspend fun authorize(device: AuthorizedDevice): Result<Unit> = write { devices.document(device.id).set(device.toDto()) }

    override suspend fun revoke(deviceId: String): Result<Unit> = write { devices.document(deviceId).updateFields { "active" to false } }

    // The wait is only there to hear a rejection from the server, like the one a non-administrator gets.
    private suspend fun write(change: suspend () -> Unit): Result<Unit> = try {
        withTimeoutOrNull(serverWait) { change() }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAttendanceError())
    }
}
