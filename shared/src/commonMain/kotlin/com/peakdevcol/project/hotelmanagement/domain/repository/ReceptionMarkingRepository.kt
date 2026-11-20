package com.peakdevcol.project.hotelmanagement.domain.repository

import com.peakdevcol.project.hotelmanagement.domain.model.AttendanceRecord
import com.peakdevcol.project.hotelmanagement.domain.model.Mark
import com.peakdevcol.project.hotelmanagement.domain.model.MarkDelivery
import com.peakdevcol.project.hotelmanagement.domain.model.User

/** RF-ASI-11: one employee identified at the computer of reception, with their own account. */
interface ReceptionSession {
    val employee: User

    /** Read from the server: the computer is shared, so its cache says nothing about this employee. */
    suspend fun openEntry(): Result<AttendanceRecord?>
    suspend fun saveEntry(record: AttendanceRecord): Result<MarkDelivery>
    suspend fun saveExit(recordId: String, exit: Mark): Result<MarkDelivery>
}

/**
 * RF-ASI-11 and V1: signs the employee in apart from the session of the computer, which stays as
 * it is, runs [action] as that employee and signs them out again.
 */
interface ReceptionMarkingRepository {
    suspend fun <T> asEmployee(email: String, password: String, action: suspend (ReceptionSession) -> Result<T>): Result<T>
}
