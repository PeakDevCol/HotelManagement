package com.peakdevcol.project.hotelmanagement.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.peakdevcol.project.hotelmanagement.domain.model.AppNavigation
import com.peakdevcol.project.hotelmanagement.domain.model.AppSection
import com.peakdevcol.project.hotelmanagement.domain.model.PendingAddition
import com.peakdevcol.project.hotelmanagement.domain.model.SectionGroup
import com.peakdevcol.project.hotelmanagement.domain.model.User
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.logout_confirm_title
import com.peakdevcol.project.hotelmanagement.resources.logout_message
import com.peakdevcol.project.hotelmanagement.resources.logout_pending_message
import com.peakdevcol.project.hotelmanagement.resources.logout_pending_title
import com.peakdevcol.project.hotelmanagement.resources.nav_more
import com.peakdevcol.project.hotelmanagement.resources.order_product_line
import com.peakdevcol.project.hotelmanagement.resources.rejected_additions_message
import com.peakdevcol.project.hotelmanagement.resources.rejected_additions_title
import com.peakdevcol.project.hotelmanagement.resources.understood
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderScreen
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddProductsScreen
import com.peakdevcol.project.hotelmanagement.ui.currentorders.label
import com.peakdevcol.project.hotelmanagement.ui.balance.BalanceScreen
import com.peakdevcol.project.hotelmanagement.ui.balance.CashClosingScreen
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersScreen
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailScreen
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ConfirmDialog
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticNavigationBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.NavigationDestination
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.NavigationSection
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SideNavigation
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskDialog
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeeFormScreen
import com.peakdevcol.project.hotelmanagement.ui.employees.EmployeesScreen
import com.peakdevcol.project.hotelmanagement.ui.employees.label
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.TaxesScreen
import com.peakdevcol.project.hotelmanagement.ui.settings.SettingsScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** A destination of the bottom bar of the phone: a section, or "Más" with the rest. */
private sealed interface BarItem {
    data class Section(val section: AppSection) : BarItem
    data object More : BarItem
}

/**
 * RF-AUTH-05 and RF-EMP-05: the app of a logged-in employee, without an intermediate menu. It opens
 * on the first section of their area. The phone has a bottom bar; wider windows a side navigation,
 * compact up to [MajesticSizes.expandedWidth].
 */
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val employee = state.employee
    val navigation = state.navigation
    if (employee == null || navigation == null) {
        MajesticLoading()
        return
    }
    CompositionLocalProvider(LocalAppMessages provides rememberAppMessages()) {
        MainLayout(state = state, employee = employee, navigation = navigation, onIntent = viewModel::onIntent)
    }
    MainDialogs(state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun MainLayout(state: MainState, employee: User, navigation: AppNavigation, onIntent: (MainIntent) -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val phone = maxWidth < MajesticSizes.mediumWidth
        val compactSideNavigation = maxWidth < MajesticSizes.expandedWidth
        CompositionLocalProvider(LocalWideWindow provides !phone) {
            // The navigation host stays in the same place for every width, so a resized window keeps its screens.
            Row(modifier = Modifier.fillMaxSize()) {
                if (!phone) {
                    SideNavigation(
                        sections = sideSections(navigation, state.pendingOrderCount),
                        selected = destination?.section() ?: navigation.startSection,
                        onSelect = { section -> navController.openSection(section.route(), navigation) },
                        employeeName = employee.fullName,
                        employeeArea = stringResource(employee.area.label()),
                        onLogOut = { onIntent(MainIntent.LogOutClick) },
                        compact = compactSideNavigation,
                    )
                }
                ScreensWithBottomBar(
                    navController = navController,
                    destination = destination,
                    showsBar = phone && (destination == null || destination.isSectionStart()),
                    navigation = navigation,
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ScreensWithBottomBar(
    navController: NavHostController,
    destination: NavDestination?,
    showsBar: Boolean,
    navigation: AppNavigation,
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.systemBars.only(if (showsBar) WindowInsetsSides.Bottom else WindowInsetsSides.Start)),
        ) {
            MainNavHost(navController = navController, navigation = navigation, state = state, onIntent = onIntent)
        }
        if (showsBar) {
            val selected = barItemOf(destination, navigation)
            MajesticNavigationBar(
                destinations = barDestinations(navigation, state.pendingOrderCount),
                selected = selected,
                onSelect = { item ->
                    // Touching the destination already open goes back to its start.
                    if (item == selected) navController.popBackStack(item.route(), inclusive = false)
                    else navController.openSection(item.route(), navigation)
                },
            )
        }
    }
}

@Composable
private fun MainNavHost(navController: NavHostController, navigation: AppNavigation, state: MainState, onIntent: (MainIntent) -> Unit) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(navController = navController, startDestination = navigation.startSection.route()) {
        composable<OrdersRoute> {
            CurrentOrdersScreen(
                onNewOrder = { navController.navigate(NewOrderRoute) },
                onOpenOrder = { orderId -> navController.navigate(OrderDetailRoute(orderId)) },
                onAddProducts = { orderId -> navController.navigate(AddProductsRoute(orderId)) },
            )
        }
        composable<OrderDetailRoute> { entry ->
            val orderId = entry.toRoute<OrderDetailRoute>().orderId
            OrderDetailScreen(orderId = orderId, onBack = back, onAddProducts = { navController.navigate(AddProductsRoute(orderId)) })
        }
        composable<NewOrderRoute> { AddOrderScreen(onClose = back) }
        composable<AddProductsRoute> { entry -> AddProductsScreen(orderId = entry.toRoute<AddProductsRoute>().orderId, onClose = back) }
        composable<BalanceRoute> { BalanceScreen(onCashClosingClick = { navController.navigate(CashClosingRoute) }) }
        composable<CashClosingRoute> { CashClosingScreen(onBack = back) }
        composable<InventoryRoute> {
            InventoryScreen(
                onOpenProduct = { productId -> navController.navigate(ProductFormRoute(productId)) },
                onCategoriesClick = { navController.navigate(CategoriesRoute) },
                onTaxesClick = { navController.navigate(TaxesRoute) },
            )
        }
        composable<ProductFormRoute> { entry -> ProductFormScreen(productId = entry.toRoute<ProductFormRoute>().productId, onBack = back) }
        composable<CategoriesRoute> { CategoriesScreen(onBack = back) }
        composable<TaxesRoute> { TaxesScreen(onBack = back) }
        composable<EmployeesRoute> {
            EmployeesScreen(onOpenEmployee = { employeeId -> navController.navigate(EmployeeFormRoute(employeeId)) })
        }
        composable<EmployeeFormRoute> { entry -> EmployeeFormScreen(employeeId = entry.toRoute<EmployeeFormRoute>().employeeId, onBack = back) }
        composable<SettingsRoute> { SettingsScreen() }
        composable<MoreRoute> {
            MoreScreen(
                state = state,
                onOpenSection = { section -> navController.navigate(section.route()) },
                onIntent = onIntent,
            )
        }
    }
}

/** Opens a section the way tabs do: its screens are kept, and back from it goes to the start section. */
private fun NavHostController.openSection(route: Any, navigation: AppNavigation) {
    navigate(route) {
        popUpTo(navigation.startSection.route()) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun BarItem.route(): Any = when (this) {
    is BarItem.Section -> section.route()
    BarItem.More -> MoreRoute
}

/** A section opened from "Más" keeps "Más" marked, because it is not in the bar. */
private fun barItemOf(destination: NavDestination?, navigation: AppNavigation): BarItem {
    val section = destination?.section() ?: return if (destination == null) BarItem.Section(navigation.startSection) else BarItem.More
    return if (section in navigation.mainSections) BarItem.Section(section) else BarItem.More
}

@Composable
private fun barDestinations(navigation: AppNavigation, pendingOrderCount: Int): List<NavigationDestination<BarItem>> =
    navigation.mainSections.map { section ->
        NavigationDestination<BarItem>(
            key = BarItem.Section(section),
            label = stringResource(section.shortTitle()),
            icon = section.icon(),
            selectedIcon = section.selectedIcon(),
            badgeCount = if (section == AppSection.ORDERS) pendingOrderCount else 0,
        )
    } + NavigationDestination(
        key = BarItem.More,
        label = stringResource(Res.string.nav_more),
        icon = MajesticIcons.menu,
        selectedIcon = MajesticIcons.menu,
    )

@Composable
private fun sideSections(navigation: AppNavigation, pendingOrderCount: Int): List<NavigationSection<AppSection>> =
    SectionGroup.entries.mapNotNull { group ->
        val sections = navigation.sections.filter { it.group == group }
        if (sections.isEmpty()) return@mapNotNull null
        NavigationSection(
            title = stringResource(group.title()),
            destinations = sections.map { section ->
                NavigationDestination(
                    key = section,
                    label = stringResource(section.title()),
                    icon = section.icon(),
                    selectedIcon = section.selectedIcon(),
                    badgeCount = if (section == AppSection.ORDERS) pendingOrderCount else 0,
                )
            },
        )
    }

@Composable
private fun MainDialogs(state: MainState, onIntent: (MainIntent) -> Unit) {
    val dismiss = { onIntent(MainIntent.LogOutDialogDismiss) }
    when (state.logOutDialog) {
        LogOutDialog.Confirmation -> ConfirmDialog(
            title = stringResource(Res.string.logout_confirm_title),
            message = stringResource(Res.string.logout_message),
            confirmText = stringResource(Res.string.logout),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onIntent(MainIntent.LogOutConfirm) },
            onDismiss = dismiss,
        )
        // RF-AUTH-03: logging out is not allowed while orders wait to be sent.
        LogOutDialog.PendingOrders -> TaskDialog(title = stringResource(Res.string.logout_pending_title), onDismiss = dismiss) {
            Text(text = stringResource(Res.string.logout_pending_message), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            MajesticButton(text = stringResource(Res.string.understood), onClick = dismiss, modifier = Modifier.fillMaxWidth())
        }
        null -> Unit
    }
    if (state.rejectedAdditions.isNotEmpty()) {
        RejectedAdditionsDialog(additions = state.rejectedAdditions, onRead = { onIntent(MainIntent.RejectedAdditionsRead) })
    }
}

/** RF-CUR-13: which products the server did not add, because their order was no longer in progress. */
@Composable
private fun RejectedAdditionsDialog(additions: List<PendingAddition>, onRead: () -> Unit) {
    TaskDialog(title = stringResource(Res.string.rejected_additions_title), onDismiss = onRead) {
        // The dialog scrolls when the list is long.
        Column(verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3)) {
            Text(text = stringResource(Res.string.rejected_additions_message), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            additions.forEach { addition ->
                Column {
                    SectionLabel(text = addition.destination.label())
                    addition.products.forEach { product ->
                        Text(
                            text = stringResource(Res.string.order_product_line, product.name, product.quantity),
                            style = MajesticTheme.typography.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        MajesticButton(text = stringResource(Res.string.understood), onClick = onRead, modifier = Modifier.fillMaxWidth())
    }
}
