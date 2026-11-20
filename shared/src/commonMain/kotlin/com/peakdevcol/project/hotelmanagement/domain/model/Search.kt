package com.peakdevcol.project.hotelmanagement.domain.model

private val UNACCENTED = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')

/** The text as searches compare it: in lower case and without accents, so "Andres" finds "Andrés". */
fun String.searchable(): String = lowercase().map { UNACCENTED[it] ?: it }.joinToString("")

/** RF-ORD-01: the products whose name contains [query], keeping their groups and leaving out the empty ones. */
fun List<CatalogGroup>.matching(query: String): List<CatalogGroup> {
    val text = query.trim().searchable()
    if (text.isEmpty()) return this
    return map { group -> group.copy(products = group.products.filter { it.name.searchable().contains(text) }) }
        .filter { it.products.isNotEmpty() }
}
