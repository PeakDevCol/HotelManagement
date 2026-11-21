package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import kotlinx.coroutines.flow.Flow

/** RF-ASI-03 to RF-ASI-05: the marks at `Attendance` of the logged-in employee. */
interface AttendanceRepository {
    /** The entry without exit of the employee. It reads the device too, so it works without connection. */
    fun observeOpenEntry(employeeId: String): Flow<AttendanceRecord?>

    /** Without connection the entry waits on the device and goes out by itself later (RF-ASI-05). */
    suspend fun saveEntry(record: AttendanceRecord): Result<MarkDelivery>

    suspend fun saveExit(recordId: String, exit: Mark): Result<MarkDelivery>
}
