package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.category_none
import com.peakdevcol.project.hotelmanagement.resources.catalog_empty_title
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.order_catalog_empty
import com.peakdevcol.project.hotelmanagement.resources.order_catalog_error
import com.peakdevcol.project.hotelmanagement.resources.product_not_found
import com.peakdevcol.project.hotelmanagement.resources.products_search_hint
import com.peakdevcol.project.hotelmanagement.resources.products_search_placeholder
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTabs
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ProductTile
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ProductTileMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SearchField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * RF-ORD-01: the products as a grid, added with one touch, grouped by category or found by name.
 * [header] goes above the search, like the destination of a new order.
 */
@Composable
fun ProductPickerContent(
    picker: ProductPickerState,
    onIntent: (ProductPickerIntent) -> Unit,
    onRetryCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(ProductTileMinWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MajesticSpacing.space4),
        horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        if (header != null) wide(key = "header") { header() }
        when {
            picker.catalogFailed -> wide(key = "error") {
                EmptyState(
                    icon = MajesticIcons.error,
                    title = stringResource(Res.string.order_catalog_error),
                    message = stringResource(Res.string.error_check_connection),
                    actionText = stringResource(Res.string.retry),
                    onAction = onRetryCatalog,
                )
            }
            !picker.catalogLoaded -> wide(key = "loading") { MajesticLoading() }
            picker.isCatalogEmpty -> wide(key = "empty") {
                EmptyState(
                    icon = MajesticIcons.inventory,
                    title = stringResource(Res.string.catalog_empty_title),
                    message = stringResource(Res.string.order_catalog_empty),
                )
            }
            else -> products(picker = picker, onIntent = onIntent)
        }
    }
}

private fun LazyGridScope.products(picker: ProductPickerState, onIntent: (ProductPickerIntent) -> Unit) {
    wide(key = "search") {
        SearchField(
            query = picker.query,
            onQueryChange = { onIntent(ProductPickerIntent.Search(it)) },
            placeholder = stringResource(Res.string.products_search_placeholder),
        )
    }
    if (!picker.isSearching && picker.catalog.size > 1) {
        wide(key = "categories") {
            val none = stringResource(Res.string.category_none)
            MajesticTabs(
                tabs = picker.catalog.map { it.categoryName ?: none },
                selectedIndex = picker.categoryIndex,
                onSelect = { onIntent(ProductPickerIntent.SelectCategory(it)) },
            )
        }
    }
    val products = picker.visibleProducts
    if (products.isEmpty()) {
        wide(key = "not-found") {
            EmptyState(
                icon = MajesticIcons.search,
                title = stringResource(Res.string.product_not_found),
                message = stringResource(Res.string.products_search_hint),
            )
        }
        return
    }
    items(products, key = { "product-${it.id}" }) { product ->
        ProductTile(
            name = product.name,
            price = product.unitPrice,
            quantity = picker.selection.quantityOf(product.id),
            onAdd = { onIntent(ProductPickerIntent.Add(product)) },
            onIncrease = { onIntent(ProductPickerIntent.Increase(product.id)) },
            onDecrease = { onIntent(ProductPickerIntent.Decrease(product.id)) },
            onRemove = { onIntent(ProductPickerIntent.Remove(product.id)) },
        )
    }
}

/** An item that takes the whole width of the grid. */
private fun LazyGridScope.wide(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}
