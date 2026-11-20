package com.peakdevcol.project.hotelmanagement.ui.addorder

import com.peakdevcol.project.hotelmanagement.domain.model.CatalogGroup
import com.peakdevcol.project.hotelmanagement.domain.model.Product
import com.peakdevcol.project.hotelmanagement.domain.model.ProductSelection
import com.peakdevcol.project.hotelmanagement.domain.model.matching

/** RF-ORD-01: the catalog, the search and the products chosen, shared by a new order and by adding to one. */
data class ProductPickerState(
    val catalog: List<CatalogGroup> = emptyList(),
    val catalogLoaded: Boolean = false,
    val catalogFailed: Boolean = false,
    val query: String = "",
    /** The category shown when there is no search, in the order the administrator gave them (RF-INV-04). */
    val categoryIndex: Int = 0,
    val selection: ProductSelection = ProductSelection(),
) {
    /** The inventory has nothing that can be ordered yet: an administrator has to create the products. */
    val isCatalogEmpty: Boolean get() = catalogLoaded && catalog.isEmpty()
    val isSearching: Boolean get() = query.isNotBlank()

    /** With a search, the matches of every category; without one, the products of the chosen category. */
    val visibleProducts: List<Product>
        get() = if (isSearching) catalog.matching(query).flatMap { it.products } else catalog.getOrNull(categoryIndex)?.products.orEmpty()

    fun withCatalog(catalog: List<CatalogGroup>): ProductPickerState =
        copy(catalog = catalog, catalogLoaded = true, catalogFailed = false, categoryIndex = categoryIndex.coerceIn(0, maxOf(0, catalog.lastIndex)))
}

sealed interface ProductPickerIntent {
    data class Search(val query: String) : ProductPickerIntent
    data class SelectCategory(val index: Int) : ProductPickerIntent
    data class Add(val product: Product) : ProductPickerIntent
    data class Increase(val productId: String) : ProductPickerIntent
    data class Decrease(val productId: String) : ProductPickerIntent
    data class Remove(val productId: String) : ProductPickerIntent
}

private const val MAX_QUERY_LENGTH = 40

fun ProductPickerState.after(intent: ProductPickerIntent): ProductPickerState = when (intent) {
    is ProductPickerIntent.Search -> copy(query = intent.query.take(MAX_QUERY_LENGTH))
    is ProductPickerIntent.SelectCategory -> copy(categoryIndex = intent.index.coerceIn(0, maxOf(0, catalog.lastIndex)))
    is ProductPickerIntent.Add -> copy(selection = selection.add(intent.product))
    is ProductPickerIntent.Increase -> copy(selection = selection.increase(intent.productId))
    is ProductPickerIntent.Decrease -> copy(selection = selection.decrease(intent.productId))
    is ProductPickerIntent.Remove -> copy(selection = selection.remove(intent.productId))
}
