package com.peakdevcol.project.hotelmanagement.domain.usecase

import com.peakdevcol.project.hotelmanagement.domain.model.SHIFT_LOOKBACK
import com.peakdevcol.project.hotelmanagement.domain.model.Shift
import com.peakdevcol.project.hotelmanagement.domain.repository.AuthRepository
import com.peakdevcol.project.hotelmanagement.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** RF-ASI-02: how far ahead an employee sees their shifts. */
val MY_SCHEDULE_SPAN = 14.days

/** RF-ASI-02: the shifts of the logged-in employee from now to two weeks ahead, also the one going on. */
class ObserveMyScheduleUseCase(
    private val authRepository: AuthRepository,
    private val shiftRepository: ShiftRepository,
    private val clock: Clock = Clock.System,
) {
    operator fun invoke(): Flow<List<Shift>> {
        val employeeId = authRepository.currentUserId() ?: return flowOf(emptyList())
        val now = clock.now()
        return shiftRepository.observeShiftsOf(employeeId, now - SHIFT_LOOKBACK, now + MY_SCHEDULE_SPAN)
            .map { shifts -> shifts.filter { it.endAt > now }.sortedBy { it.startAt } }
    }
}
