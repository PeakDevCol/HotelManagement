package com.peakdevcol.project.hotelmanagement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.peakdevcol.project.hotelmanagement.platformName
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.access_denied_message
import com.peakdevcol.project.hotelmanagement.resources.access_denied_title
import com.peakdevcol.project.hotelmanagement.resources.app_name
import com.peakdevcol.project.hotelmanagement.resources.firebase_not_configured
import com.peakdevcol.project.hotelmanagement.resources.logout
import com.peakdevcol.project.hotelmanagement.resources.running_on
import com.peakdevcol.project.hotelmanagement.resources.session_error
import com.peakdevcol.project.hotelmanagement.ui.components.AuthFrame
import com.peakdevcol.project.hotelmanagement.ui.components.PrimaryButton
import com.peakdevcol.project.hotelmanagement.ui.addorder.AddOrderScreen
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CurrentOrdersScreen
import com.peakdevcol.project.hotelmanagement.ui.currentorders.OrderDetailScreen
import com.peakdevcol.project.hotelmanagement.ui.home.ComingSoonScreen
import com.peakdevcol.project.hotelmanagement.ui.home.HomeScreen
import com.peakdevcol.project.hotelmanagement.ui.home.HomeSection
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.CategoriesScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.InventoryScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.ProductFormScreen
import com.peakdevcol.project.hotelmanagement.ui.inventory.TaxesScreen
import com.peakdevcol.project.hotelmanagement.ui.login.LoginScreen
import com.peakdevcol.project.hotelmanagement.ui.theme.HotelManagementTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Navigation destinations. Each migrated screen adds its own route.
@Serializable
data object IntroductionRoute

@Serializable
data object LoginRoute

@Serializable
data object HomeRoute

@Serializable
data object AddOrderRoute

@Serializable
data object CurrentOrdersRoute

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
data class OrderDetailRoute(val orderId: String)

/**
 * A menu section that is not migrated yet.
 * The section travels by name: outside Android, navigation has no argument type for enums
 * and the whole graph fails to build, which left the desktop app stuck after the login.
 */
@Serializable
data class ComingSoonRoute(val sectionName: String)

@Composable
fun App() {
    HotelManagementTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val appViewModel: AppViewModel = koinViewModel()
            val appState by appViewModel.state.collectAsStateWithLifecycle()

            when (val state = appState) {
                AppState.Loading -> LoadingScreen()
                AppState.LoggedOut -> AuthNavigation()
                is AppState.LoggedIn -> HomeNavigation()
                AppState.AccessDenied ->
                    AccessDeniedScreen(onLogOutClick = { appViewModel.onIntent(AppIntent.LogOut) })
                AppState.FirebaseNotConfigured ->
                    MessageScreen(stringResource(Res.string.firebase_not_configured))
                is AppState.Error ->
                    MessageScreen(stringResource(Res.string.session_error, state.message))
            }
        }
    }
}

/** Screens available without a session. Logging in swaps to [HomeNavigation] automatically. */
@Composable
private fun AuthNavigation() {
    val navController = rememberNavController()
    AuthFrame {
        NavHost(navController = navController, startDestination = IntroductionRoute) {
            composable<IntroductionRoute> {
                IntroductionScreen(onLoginClick = { navController.navigate(LoginRoute) })
            }
            composable<LoginRoute> {
                LoginScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Screens for a logged-in employee. Logging out swaps back to [AuthNavigation]. */
@Composable
private fun HomeNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(onSectionClick = { section ->
                when (section) {
                    HomeSection.AddOrder -> navController.navigate(AddOrderRoute)
                    HomeSection.CurrentOrders -> navController.navigate(CurrentOrdersRoute)
                    HomeSection.Inventory -> navController.navigate(InventoryRoute)
                    else -> navController.navigate(ComingSoonRoute(section.name))
                }
            })
        }
        composable<AddOrderRoute> {
            AddOrderScreen(onBack = { navController.popBackStack() })
        }
        composable<CurrentOrdersRoute> {
            CurrentOrdersScreen(
                onBack = { navController.popBackStack() },
                onOrderClick = { orderId -> navController.navigate(OrderDetailRoute(orderId)) },
            )
        }
        composable<OrderDetailRoute> { entry ->
            OrderDetailScreen(
                orderId = entry.toRoute<OrderDetailRoute>().orderId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<InventoryRoute> {
            InventoryScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId -> navController.navigate(ProductFormRoute(productId)) },
                onCategoriesClick = { navController.navigate(CategoriesRoute) },
                onTaxesClick = { navController.navigate(TaxesRoute) },
            )
        }
        composable<ProductFormRoute> { entry ->
            ProductFormScreen(
                productId = entry.toRoute<ProductFormRoute>().productId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<CategoriesRoute> {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable<TaxesRoute> {
            TaxesScreen(onBack = { navController.popBackStack() })
        }
        composable<ComingSoonRoute> { entry ->
            ComingSoonScreen(
                section = HomeSection.valueOf(entry.toRoute<ComingSoonRoute>().sectionName),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** RF-AUTH-07: the account has a session but no active employee profile. */
@Composable
private fun AccessDeniedScreen(onLogOutClick: () -> Unit) {
    AuthFrame {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.access_denied_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.access_denied_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                text = stringResource(Res.string.logout),
                onClick = onLogOutClick,
                modifier = Modifier.widthIn(max = 480.dp),
            )
        }
    }
}

@Composable
private fun MessageScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = stringResource(Res.string.running_on, platformName()))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
