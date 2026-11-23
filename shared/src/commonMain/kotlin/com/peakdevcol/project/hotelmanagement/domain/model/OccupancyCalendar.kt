package com.peakdevcol.project.hotelmanagement.domain.model

/** RF-RES-04: how many days the calendar shows. */
enum class OccupancySpan {
    TWO_WEEKS,

    /** The whole calendar month. */
    MONTH,
}

/** The days the calendar shows, from [firstDay] to [lastDay]. */
data class OccupancyPeriod(val span: OccupancySpan, val firstDay: CalendarDate) {
    val lastDay: CalendarDate
        get() = when (span) {
            OccupancySpan.TWO_WEEKS -> firstDay.plusDays(TWO_WEEKS_DAYS - 1L)
            OccupancySpan.MONTH -> firstDay.lastDayOfMonth
        }

    val days: List<CalendarDate> get() = (0..(lastDay.epochDay - firstDay.epochDay)).map { firstDay.plusDays(it) }

    fun previous(): OccupancyPeriod = when (span) {
        OccupancySpan.TWO_WEEKS -> copy(firstDay = firstDay.plusDays(-TWO_WEEKS_DAYS.toLong()))
        OccupancySpan.MONTH -> copy(firstDay = firstDay.plusDays(-1).firstDayOfMonth)
    }

    fun next(): OccupancyPeriod = copy(firstDay = lastDay.plusDays(1))

    /** The same moment seen in another span: the month of the first day, or two weeks from today if it is in that month. */
    fun inSpan(newSpan: OccupancySpan, today: CalendarDate): OccupancyPeriod = when (newSpan) {
        span -> this
        OccupancySpan.MONTH -> OccupancyPeriod(OccupancySpan.MONTH, firstDay.firstDayOfMonth)
        OccupancySpan.TWO_WEEKS -> OccupancyPeriod(OccupancySpan.TWO_WEEKS, if (today in firstDay..lastDay) today else firstDay)
    }

    companion object {
        const val TWO_WEEKS_DAYS = 14

        /** Decision 2A of the third PR of M11: the calendar opens on two weeks from today. */
        fun from(today: CalendarDate, span: OccupancySpan = OccupancySpan.TWO_WEEKS) = when (span) {
            OccupancySpan.TWO_WEEKS -> OccupancyPeriod(span, today)
            OccupancySpan.MONTH -> OccupancyPeriod(span, today.firstDayOfMonth)
        }
    }
}

/** A row of the calendar: a room and the reservations that hold its nights in the period. */
data class OccupancyRoom(
    val room: Room,
    /** Null when the type no longer exists. */
    val typeName: String?,
    /** By arrival. */
    val reservations: List<Reservation>,
)

/** RF-RES-04: the rooms by day, with each reservation that holds them. */
data class OccupancyCalendar(
    val period: OccupancyPeriod,
    val rooms: List<OccupancyRoom>,
) {
    fun reservationOf(reservationId: String): Reservation? =
        rooms.asSequence().flatMap { it.reservations }.firstOrNull { it.id == reservationId }
}

/** Whether the reservation holds at least one night of the period. */
fun Reservation.holdsANightIn(period: OccupancyPeriod): Boolean = stay.checkIn <= period.lastDay && stay.checkOut > period.firstDay

/**
 * Decision 1A of the third PR of M11: a free night touched on the calendar starts a new
 * reservation in that room, from that night to the morning after.
 */
data class ReservationStart(val roomId: String, val checkIn: CalendarDate)
