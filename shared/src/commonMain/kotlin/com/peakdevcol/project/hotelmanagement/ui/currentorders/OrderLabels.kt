package com.peakdevcol.project.hotelmanagement.ui.currentorders

import androidx.compose.runtime.Composable
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.domain.model.OrderListing
import com.peakdevcol.project.hotelmanagement.domain.model.OrderState
import com.peakdevcol.project.hotelmanagement.domain.model.PaymentMethod
import com.peakdevcol.project.hotelmanagement.domain.model.formatBogotaDateTime
import com.peakdevcol.project.hotelmanagement.domain.model.minutesBetween
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.destination_bar
import com.peakdevcol.project.hotelmanagement.resources.destination_bar_number
import com.peakdevcol.project.hotelmanagement.resources.destination_other
import com.peakdevcol.project.hotelmanagement.resources.destination_pool
import com.peakdevcol.project.hotelmanagement.resources.destination_pool_number
import com.peakdevcol.project.hotelmanagement.resources.destination_room
import com.peakdevcol.project.hotelmanagement.resources.destination_room_number
import com.peakdevcol.project.hotelmanagement.resources.destination_table
import com.peakdevcol.project.hotelmanagement.resources.destination_table_number
import com.peakdevcol.project.hotelmanagement.resources.order_destination_default
import com.peakdevcol.project.hotelmanagement.resources.order_employee_unknown
import com.peakdevcol.project.hotelmanagement.resources.order_payment_method_cash
import com.peakdevcol.project.hotelmanagement.resources.order_payment_method_transfer
import com.peakdevcol.project.hotelmanagement.resources.order_product_count
import com.peakdevcol.project.hotelmanagement.resources.order_state_cancelled
import com.peakdevcol.project.hotelmanagement.resources.order_state_closed
import com.peakdevcol.project.hotelmanagement.resources.order_state_in_progress
import com.peakdevcol.project.hotelmanagement.resources.order_state_paid
import com.peakdevcol.project.hotelmanagement.resources.time_hours_ago
import com.peakdevcol.project.hotelmanagement.resources.time_hours_ago_exact
import com.peakdevcol.project.hotelmanagement.resources.time_just_now
import com.peakdevcol.project.hotelmanagement.resources.time_minutes_ago
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

fun OrderState.label(): StringResource = when (this) {
    OrderState.IN_PROGRESS -> Res.string.order_state_in_progress
    OrderState.CLOSED -> Res.string.order_state_closed
    OrderState.PAID -> Res.string.order_state_paid
    OrderState.CANCELLED -> Res.string.order_state_cancelled
}

fun PaymentMethod.label(): StringResource = when (this) {
    PaymentMethod.CASH -> Res.string.order_payment_method_cash
    PaymentMethod.TRANSFER -> Res.string.order_payment_method_transfer
}

fun DestinationKind.label(): StringResource = when (this) {
    DestinationKind.ROOM -> Res.string.destination_room
    DestinationKind.TABLE -> Res.string.destination_table
    DestinationKind.POOL -> Res.string.destination_pool
    DestinationKind.BAR -> Res.string.destination_bar
    DestinationKind.OTHER -> Res.string.destination_other
}

/** The destination as the hotel says it: "Mesa 4", "Habitación 204", "Piscina". */
@Composable
fun OrderDestination?.label(): String = when (this) {
    is OrderDestination.Room -> stringResource(Res.string.destination_room_number, number)
    is OrderDestination.Table -> stringResource(Res.string.destination_table_number, number)
    is OrderDestination.Pool ->
        number?.let { stringResource(Res.string.destination_pool_number, it) } ?: stringResource(Res.string.destination_pool)
    is OrderDestination.Bar ->
        number?.let { stringResource(Res.string.destination_bar_number, it) } ?: stringResource(Res.string.destination_bar)
    OrderDestination.Other -> stringResource(Res.string.destination_other)
    null -> stringResource(Res.string.order_destination_default)
}

@Composable
fun employeeNameOrUnknown(name: String?): String = name ?: stringResource(Res.string.order_employee_unknown)

/** "3 productos": the units of the order, not its lines. */
@Composable
fun productCountLabel(units: Int): String = pluralStringResource(Res.plurals.order_product_count, units, units)

/** Who took the order and how many products it has: "Andrés · 3 productos". */
@Composable
fun OrderListing.summary(): String =
    listOf(employeeNameOrUnknown(employeeName), productCountLabel(order.products.sumOf { it.quantity })).joinToString(" · ")

/** How long ago something happened, in the words of the lists: "Hace 12 min". After a day, the date. */
@Composable
fun elapsedLabel(since: Instant?, now: Instant): String {
    if (since == null) return stringResource(Res.string.time_just_now)
    val minutes = minutesBetween(since, now)
    val hours = minutes / MINUTES_PER_HOUR
    val remainingMinutes = minutes % MINUTES_PER_HOUR
    return when {
        minutes < 1 -> stringResource(Res.string.time_just_now)
        minutes < MINUTES_PER_HOUR -> stringResource(Res.string.time_minutes_ago, minutes)
        minutes >= MINUTES_PER_DAY -> formatBogotaDateTime(since)
        remainingMinutes == 0L -> stringResource(Res.string.time_hours_ago_exact, hours)
        else -> stringResource(Res.string.time_hours_ago, hours, remainingMinutes)
    }
}
