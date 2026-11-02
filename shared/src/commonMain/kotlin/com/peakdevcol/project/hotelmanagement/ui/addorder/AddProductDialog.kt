package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.add
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.error_quantity
import com.peakdevcol.project.hotelmanagement.resources.order_add_product
import com.peakdevcol.project.hotelmanagement.resources.product
import com.peakdevcol.project.hotelmanagement.resources.product_not_found
import com.peakdevcol.project.hotelmanagement.resources.quantity
import org.jetbrains.compose.resources.stringResource

const val MAX_QUANTITY = 99

/** Pick a product from the catalog, filtered by name, and how many units to add. */
@Composable
fun AddProductDialog(
    catalog: List<Product>,
    onAdd: (Product, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var quantityText by rememberSaveable { mutableStateOf("1") }

    val filtered = remember(catalog, query) {
        catalog.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val selected = catalog.firstOrNull { it.id == selectedId }
    val quantity = quantityText.toIntOrNull()?.takeIf { it in 1..MAX_QUANTITY }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.order_add_product)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.product)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.product_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(filtered, key = { it.id }) { product ->
                            ProductOption(
                                product = product,
                                selected = product.id == selectedId,
                                onClick = { selectedId = product.id },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { text -> quantityText = text.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(Res.string.quantity)) },
                    isError = quantity == null,
                    supportingText = if (quantity == null) {
                        { Text(stringResource(Res.string.error_quantity, MAX_QUANTITY)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected != null && quantity != null) onAdd(selected, quantity) },
                enabled = selected != null && quantity != null,
            ) { Text(stringResource(Res.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

@Composable
private fun ProductOption(product: Product, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = product.name,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatPesos(product.unitPrice),
            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
