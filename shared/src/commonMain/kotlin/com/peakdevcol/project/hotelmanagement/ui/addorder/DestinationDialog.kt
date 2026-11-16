package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.destination_bar
import com.peakdevcol.project.hotelmanagement.resources.destination_bar_number
import com.peakdevcol.project.hotelmanagement.resources.destination_number
import com.peakdevcol.project.hotelmanagement.resources.destination_number_optional
import com.peakdevcol.project.hotelmanagement.resources.destination_other
import com.peakdevcol.project.hotelmanagement.resources.destination_pool
import com.peakdevcol.project.hotelmanagement.resources.destination_pool_number
import com.peakdevcol.project.hotelmanagement.resources.destination_room
import com.peakdevcol.project.hotelmanagement.resources.destination_room_number
import com.peakdevcol.project.hotelmanagement.resources.destination_table
import com.peakdevcol.project.hotelmanagement.resources.destination_table_number
import com.peakdevcol.project.hotelmanagement.resources.dialog_accept
import com.peakdevcol.project.hotelmanagement.resources.order_destination_default
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class NumberRule { Required, Optional, NotUsed }

private enum class DestinationType(val label: StringResource, val numberRule: NumberRule) {
    Room(Res.string.destination_room, NumberRule.Required),
    Table(Res.string.destination_table, NumberRule.Required),
    Pool(Res.string.destination_pool, NumberRule.Optional),
    Bar(Res.string.destination_bar, NumberRule.Optional),
    Other(Res.string.destination_other, NumberRule.NotUsed),
}

/** Choose a room, a table, the pool, the bar or "other" as the order destination. */
@Composable
fun DestinationDialog(
    current: OrderDestination?,
    onSelect: (OrderDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by rememberSaveable {
        mutableStateOf(
            when (current) {
                is OrderDestination.Room -> DestinationType.Room
                is OrderDestination.Table -> DestinationType.Table
                is OrderDestination.Pool -> DestinationType.Pool
                is OrderDestination.Bar -> DestinationType.Bar
                OrderDestination.Other -> DestinationType.Other
                null -> null
            },
        )
    }
    var numberText by rememberSaveable {
        mutableStateOf(
            when (current) {
                is OrderDestination.Room -> current.number.toString()
                is OrderDestination.Table -> current.number.toString()
                is OrderDestination.Pool -> current.number?.toString().orEmpty()
                is OrderDestination.Bar -> current.number?.toString().orEmpty()
                OrderDestination.Other, null -> ""
            },
        )
    }

    val number = numberText.toIntOrNull()?.takeIf { it > 0 }
    // A number that was typed but is not valid must not be dropped silently.
    val optionalNumberIsValid = numberText.isEmpty() || number != null
    val destination = when (type) {
        DestinationType.Room -> number?.let { OrderDestination.Room(it) }
        DestinationType.Table -> number?.let { OrderDestination.Table(it) }
        DestinationType.Pool -> OrderDestination.Pool(number).takeIf { optionalNumberIsValid }
        DestinationType.Bar -> OrderDestination.Bar(number).takeIf { optionalNumberIsValid }
        DestinationType.Other -> OrderDestination.Other
        null -> null
    }
    val numberLabel = when (type?.numberRule) {
        NumberRule.Optional -> Res.string.destination_number_optional
        else -> Res.string.destination_number
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.order_destination_default)) },
        text = {
            Column {
                Column(modifier = Modifier.selectableGroup()) {
                    DestinationType.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = type == option,
                                    onClick = { type = option },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = type == option, onClick = null)
                            Text(stringResource(option.label), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = numberText,
                    onValueChange = { text -> numberText = text.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(numberLabel)) },
                    enabled = type != null && type?.numberRule != NumberRule.NotUsed,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { destination?.let(onSelect) }, enabled = destination != null) {
                Text(stringResource(Res.string.dialog_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

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
