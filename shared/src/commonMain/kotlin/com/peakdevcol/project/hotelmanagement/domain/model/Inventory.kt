package com.peakdevcol.project.hotelmanagement.domain.model

/** A group of products of the inventory. The administrator defines them and their order (RF-INV-04). */
data class Category(
    val id: String,
    val name: String,
    /** Where the category goes in the lists: the lowest first. */
    val position: Int,
)

/** A product as the inventory keeps it (RF-INV-01). An order copies its name and price when it is added. */
data class InventoryProduct(
    val id: String,
    val name: String,
    val unitPrice: Long,
    val categoryId: String,
    val taxId: String? = null,
    /** RF-INV-03: an inactive product is kept for the history, but cannot be ordered. */
    val active: Boolean = true,
)

/** The whole inventory, with the categories in their order. */
data class Inventory(
    val categories: List<Category>,
    val products: List<InventoryProduct>,
    val taxes: List<Tax> = emptyList(),
) {
    fun taxOf(product: InventoryProduct): Tax? = taxes.firstOrNull { it.id == product.taxId }

    /** The rate an order must copy for this product: an inactive tax no longer applies. */
    fun appliedTaxRateOf(product: InventoryProduct): Int = taxOf(product)?.takeIf { it.active }?.rate ?: 0

    fun productsOf(category: Category): List<InventoryProduct> =
        products.filter { it.categoryId == category.id }.sortedBy { it.name.lowercase() }

    /** Products whose category no longer exists. They are still shown, so nothing gets lost from sight. */
    val productsWithoutCategory: List<InventoryProduct>
        get() {
            val categoryIds = categories.map { it.id }.toSet()
            return products.filterNot { it.categoryId in categoryIds }.sortedBy { it.name.lowercase() }
        }
}

/** The products of one category that can be ordered, ready for the product picker. */
data class CatalogGroup(
    /** Null for products whose category no longer exists. */
    val categoryName: String?,
    val products: List<Product>,
)

/** Why a change to the inventory was not saved. */
sealed class InventoryError(message: String) : Exception(message) {
    data object BlankName : InventoryError("The name is required")
    data object InvalidPrice : InventoryError("The price must be greater than zero")
    data object MissingCategory : InventoryError("The product needs a category")
    data object DuplicateCategory : InventoryError("There is already a category with that name")
    data object InvalidTaxRate : InventoryError("The rate must be a percentage above 0 and up to 100")

    /** A category with products is not deleted: its products would be left without group. */
    data object CategoryInUse : InventoryError("The category still has products")
    data object NotFound : InventoryError("It no longer exists")
    data object NotAllowed : InventoryError("Only an administrator changes the inventory")
    data object Unknown : InventoryError("Unknown error")
}
