package com.peakdevcol.project.hotelmanagement.fakes

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceError
import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.AuthorizedDevice
import com.peakdevcol.project.hotelmanagement.domain.model.Correction
import com.peakdevcol.project.hotelmanagement.domain.model.CorrectionError
import com.peakdevcol.project.hotelmanagement.domain.model.HotelSettings
import com.peakdevcol.project.hotelmanagement.domain.model.LocationReading
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.MarkSource
import com.peakdevcol.project.hotelmanagement.domain.model.ReportError
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceCorrectionRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.AttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.DeviceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationProvider
import com.peakdevcol.project.hotelmanagement.domain.repository.LocationResult
import com.peakdevcol.project.hotelmanagement.domain.repository.ReceptionMarkingRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ReceptionSession
import com.peakdevcol.project.hotelmanagement.domain.repository.TeamAttendanceRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ThisDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

const val HOTEL_LATITUDE = 4.60971
const val HOTEL_LONGITUDE = -74.08175

/** The hotel with a zone of 150 m, trusting readings of 100 m or better. */
val hotelSettings = HotelSettings(hotelLatitude = HOTEL_LATITUDE, hotelLongitude = HOTEL_LONGITUDE, zoneRadiusMeters = 150, minAccuracyMeters = 100)

/** About 111 m north of the hotel for each 0.001 degrees. */
fun readingNorthOfHotel(degrees: Double = 0.0, accuracyMeters: Double = 12.0, simulated: Boolean = false) =
    LocationReading(HOTEL_LATITUDE + degrees, HOTEL_LONGITUDE, accuracyMeters, simulated)

fun phoneMark(at: String?, deviceTime: String = at!!, distanceMeters: Int = 10, accuracyMeters: Int = 12) = Mark(
    at = at?.let(Instant::parse),
    deviceTime = Instant.parse(deviceTime),
    source = MarkSource.PHONE,
    distanceMeters = distanceMeters,
    accuracyMeters = accuracyMeters,
)

fun entry(id: String, employeeId: String, at: String, clockOut: String? = null) =
    AttendanceRecord(id = id, employeeId = employeeId, clockIn = phoneMark(at), clockOut = clockOut?.let { phoneMark(it) })

/** A record by Colombian times, like `bogotaRecord("a", "ana", "2026-11-20 06:05", "2026-11-20 14:01")`. */
fun bogotaRecord(id: String, employeeId: String, entry: String, exit: String? = null) =
    entry(id, employeeId, bogotaInstant(entry), exit?.let(::bogotaInstant))

/** A moment written in Colombian time, `2026-11-20 06:05`, as the ISO text of UTC. */
fun bogotaInstant(text: String): String = Instant.parse(text.replace(' ', 'T') + ":00-05:00").toString()

class FakeAttendanceRepository(vararg records: AttendanceRecord) : AttendanceRepository {
    val records = MutableStateFlow(records.toList())
    var delivery = MarkDelivery.SENT
    var writeFailure: AttendanceError? = null
    var readFailure: Throwable? = null

    override fun observeOpenEntry(employeeId: String): Flow<AttendanceRecord?> = records.map { all ->
        readFailure?.let { throw it }
        all.filter { it.employeeId == employeeId && it.isOpen }.maxByOrNull { it.clockIn.deviceTime }
    }

    override suspend fun saveEntry(record: AttendanceRecord): Result<MarkDelivery> {
        writeFailure?.let { return Result.failure(it) }
        records.value += record
        return Result.success(delivery)
    }

    override suspend fun saveExit(recordId: String, exit: Mark): Result<MarkDelivery> {
        writeFailure?.let { return Result.failure(it) }
        records.value = records.value.map { if (it.id == recordId) it.copy(clockOut = exit) else it }
        return Result.success(delivery)
    }
}

class FakeDeviceRepository(vararg devices: AuthorizedDevice) : DeviceRepository {
    val devices = MutableStateFlow(devices.toList())
    var writeFailure: AttendanceError? = null
    var readFailure: Throwable? = null

    override fun observeDevices(): Flow<List<AuthorizedDevice>> = devices.map { readFailure?.let { failure -> throw failure } ?: it }

    override fun observeDevice(deviceId: String): Flow<AuthorizedDevice?> = observeDevices().map { all -> all.firstOrNull { it.id == deviceId } }

    override suspend fun authorize(device: AuthorizedDevice): Result<Unit> {
        writeFailure?.let { return Result.failure(it) }
        devices.value = devices.value.filterNot { it.id == device.id } + device
        return Result.success(Unit)
    }

    override suspend fun revoke(deviceId: String): Result<Unit> {
        writeFailure?.let { return Result.failure(it) }
        devices.value = devices.value.map { if (it.id == deviceId) it.copy(active = false) else it }
        return Result.success(Unit)
    }
}

class FakeThisDevice(override val id: String = "reception-pc", override val canMarkForOthers: Boolean = true) : ThisDevice

class FakeLocationProvider(
    override val isAvailable: Boolean = true,
    var permission: Boolean = true,
    var result: LocationResult = LocationResult.Found(readingNorthOfHotel()),
) : LocationProvider {
    /** The accuracy each reading was asked for. */
    val requests = mutableListOf<Double>()

    override fun hasPermission(): Boolean = permission

    override suspend fun currentLocation(desiredAccuracyMeters: Double): LocationResult {
        requests += desiredAccuracyMeters
        return result
    }
}

/** The accounts of the employees, by email, with their password; they mark on [attendance]. */
class FakeReceptionMarkingRepository(
    private val attendance: FakeAttendanceRepository,
    vararg accounts: Pair<String, User>,
) : ReceptionMarkingRepository {
    private val accounts = accounts.associate { (password, user) -> user.email to (password to user) }
    var signInFailure: AttendanceError? = null

    /** Who is signed in on the second instance right now: nobody, outside a mark. */
    var signedIn: String? = null
        private set

    override suspend fun <T> asEmployee(email: String, password: String, action: suspend (ReceptionSession) -> Result<T>): Result<T> {
        signInFailure?.let { return Result.failure(it) }
        val (expectedPassword, user) = accounts[email] ?: return Result.failure(AttendanceError.InvalidCredentials)
        if (expectedPassword != password) return Result.failure(AttendanceError.InvalidCredentials)
        if (!user.active) return Result.failure(AttendanceError.InactiveAccount)
        signedIn = user.id
        try {
            return action(Session(user))
        } finally {
            signedIn = null
        }
    }

    private inner class Session(override val employee: User) : ReceptionSession {
        override suspend fun openEntry(): Result<AttendanceRecord?> = Result.success(attendance.observeOpenEntry(employee.id).first())
        override suspend fun saveEntry(record: AttendanceRecord): Result<MarkDelivery> = attendance.saveEntry(record)
        override suspend fun saveExit(recordId: String, exit: Mark): Result<MarkDelivery> = attendance.saveExit(recordId, exit)
    }
}

/** The marks of every employee, over the records of [attendance], as Firestore holds them. */
class FakeTeamAttendanceRepository(private val attendance: FakeAttendanceRepository) : TeamAttendanceRepository {
    var readFailure: ReportError? = null
    var listenFailure: Throwable? = null

    /** The range of every reading, in order. */
    val reads = mutableListOf<Pair<Instant, Instant>>()

    override fun observeOpenEntries(): Flow<List<AttendanceRecord>> = attendance.records.map { all ->
        listenFailure?.let { throw it }
        all.filter { it.isOpen }
    }

    // By the time of the server: a mark that has not reached it is not there yet.
    override suspend fun records(from: Instant, to: Instant): Result<List<AttendanceRecord>> {
        reads += from to to
        readFailure?.let { return Result.failure(it) }
        return Result.success(attendance.records.value.filter { record -> record.clockIn.at?.let { it >= from && it < to } == true })
    }
}

/** The corrections of the administrator, over the records of [attendance]. */
class FakeAttendanceCorrectionRepository(private val attendance: FakeAttendanceRepository) : AttendanceCorrectionRepository {
    var failure: CorrectionError? = null
    val corrections = mutableListOf<Correction>()

    override suspend fun addCorrection(recordId: String, correction: Correction, missingExit: Mark?): Result<Unit> {
        failure?.let { return Result.failure(it) }
        val record = attendance.records.value.firstOrNull { it.id == recordId } ?: return Result.failure(CorrectionError.Changed)
        if (missingExit != null && record.clockOut != null) return Result.failure(CorrectionError.Changed)
        attendance.records.value = attendance.records.value.map {
            if (it.id == recordId) it.copy(clockOut = missingExit ?: it.clockOut, corrections = it.corrections + correction) else it
        }
        corrections += correction
        return Result.success(Unit)
    }

    override suspend fun register(record: AttendanceRecord): Result<Unit> {
        failure?.let { return Result.failure(it) }
        attendance.records.value += record
        corrections += record.corrections
        return Result.success(Unit)
    }
}
