package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import kotlinx.serialization.Serializable

@Serializable
data object OrdersRoute

@Serializable
data class OrderDetailRoute(val orderId: String)

@Serializable
data object NewOrderRoute

@Serializable
data class AddProductsRoute(val orderId: String)

@Serializable
data object BalanceRoute

@Serializable
data object CashClosingRoute

@Serializable
data object InventoryRoute

/** A null id opens the form to create a product. */
@Serializable
data class ProductFormRoute(val productId: String? = null)

@Serializable
data object CategoriesRoute

@Serializable
data object TaxesRoute

@Serializable
data object EmployeesRoute

/** A null id opens the form to create an employee. */
@Serializable
data class EmployeeFormRoute(val employeeId: String? = null)

@Serializable
data object SettingsRoute

/** "Más" of the phone: who is logged in, the sections that are not in the bar and logging out. */
@Serializable
data object MoreRoute

fun AppSection.route(): Any = when (this) {
    AppSection.ORDERS -> OrdersRoute
    AppSection.BALANCE -> BalanceRoute
    AppSection.INVENTORY -> InventoryRoute
    AppSection.EMPLOYEES -> EmployeesRoute
    AppSection.SETTINGS -> SettingsRoute
}

/** The section a screen belongs to, so the navigation marks it. Null for "Más". */
fun NavDestination.section(): AppSection? = when {
    hasRoute(OrdersRoute::class) || hasRoute(OrderDetailRoute::class) ||
        hasRoute(NewOrderRoute::class) || hasRoute(AddProductsRoute::class) -> AppSection.ORDERS
    hasRoute(BalanceRoute::class) || hasRoute(CashClosingRoute::class) -> AppSection.BALANCE
    hasRoute(InventoryRoute::class) || hasRoute(ProductFormRoute::class) ||
        hasRoute(CategoriesRoute::class) || hasRoute(TaxesRoute::class) -> AppSection.INVENTORY
    hasRoute(EmployeesRoute::class) || hasRoute(EmployeeFormRoute::class) -> AppSection.EMPLOYEES
    hasRoute(SettingsRoute::class) -> AppSection.SETTINGS
    else -> null
}

/** The screens the navigation leads to. The rest open from them, without the bottom bar of the phone. */
fun NavDestination.isSectionStart(): Boolean =
    hasRoute(OrdersRoute::class) || hasRoute(BalanceRoute::class) || hasRoute(InventoryRoute::class) ||
        hasRoute(EmployeesRoute::class) || hasRoute(SettingsRoute::class) || hasRoute(MoreRoute::class)
