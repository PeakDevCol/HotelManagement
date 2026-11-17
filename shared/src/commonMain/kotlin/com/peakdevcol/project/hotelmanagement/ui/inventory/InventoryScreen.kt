package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.domain.model.Tax
import com.peakdevcol.project.hotelmanagement.domain.model.formatPesos
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.category_none
import com.peakdevcol.project.hotelmanagement.resources.ic_add
import com.peakdevcol.project.hotelmanagement.resources.inventory_categories
import com.peakdevcol.project.hotelmanagement.resources.inventory_category_empty
import com.peakdevcol.project.hotelmanagement.resources.inventory_error
import com.peakdevcol.project.hotelmanagement.resources.inventory_new_product
import com.peakdevcol.project.hotelmanagement.resources.inventory_no_categories
import com.peakdevcol.project.hotelmanagement.resources.inventory_product_inactive
import com.peakdevcol.project.hotelmanagement.resources.inventory_taxes
import com.peakdevcol.project.hotelmanagement.resources.menu_inventory
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    onProductClick: (productId: String?) -> Unit,
    onCategoriesClick: () -> Unit,
    onTaxesClick: () -> Unit,
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InventoryEffect.NavigateToProduct -> onProductClick(effect.productId)
                InventoryEffect.NavigateToCategories -> onCategoriesClick()
                InventoryEffect.NavigateToTaxes -> onTaxesClick()
            }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.menu_inventory), onBack = onBack) },
        floatingActionButton = {
            if (state.canCreateProducts) {
                FloatingActionButton(onClick = { viewModel.onIntent(InventoryIntent.NewProduct) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.inventory_new_product),
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = { viewModel.onIntent(InventoryIntent.ManageCategories) }) {
                    Text(stringResource(Res.string.inventory_categories))
                }
                TextButton(onClick = { viewModel.onIntent(InventoryIntent.ManageTaxes) }) {
                    Text(stringResource(Res.string.inventory_taxes))
                }
            }
            HorizontalDivider()
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.loadFailed -> CenteredMessage(
                    message = stringResource(Res.string.inventory_error),
                    onRetry = { viewModel.onIntent(InventoryIntent.Retry) },
                )
                state.needsCategories -> CenteredMessage(stringResource(Res.string.inventory_no_categories))
                else -> InventoryList(
                    inventory = state.inventory,
                    onProductClick = { viewModel.onIntent(InventoryIntent.OpenProduct(it)) },
                )
            }
        }
    }
}

@Composable
private fun InventoryList(inventory: Inventory, onProductClick: (String) -> Unit) {
    // The bottom space keeps the last product clear of the floating button.
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        inventory.categories.forEach { category ->
            val products = inventory.productsOf(category)
            item(key = "category-${category.id}") { CategoryHeader(category.name) }
            if (products.isEmpty()) {
                item(key = "empty-${category.id}") {
                    Text(
                        text = stringResource(Res.string.inventory_category_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(products, key = { it.id }) { product ->
                ProductRow(product, tax = inventory.taxOf(product), onClick = { onProductClick(product.id) })
            }
        }
        val withoutCategory = inventory.productsWithoutCategory
        if (withoutCategory.isNotEmpty()) {
            item(key = "category-none") { CategoryHeader(stringResource(Res.string.category_none)) }
            items(withoutCategory, key = { it.id }) { product ->
                ProductRow(product, tax = inventory.taxOf(product), onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ProductRow(product: InventoryProduct, tax: Tax?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (product.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tax != null) {
                Text(
                    text = tax.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!product.active) {
                Text(
                    text = stringResource(Res.string.inventory_product_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(text = formatPesos(product.unitPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
