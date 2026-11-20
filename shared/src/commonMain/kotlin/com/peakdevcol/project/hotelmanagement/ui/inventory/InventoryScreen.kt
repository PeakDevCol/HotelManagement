package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Inventory
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryProduct
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.category_none
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.inventory_categories
import com.peakdevcol.project.hotelmanagement.resources.inventory_categories_hint
import com.peakdevcol.project.hotelmanagement.resources.inventory_category_empty
import com.peakdevcol.project.hotelmanagement.resources.inventory_create_category
import com.peakdevcol.project.hotelmanagement.resources.inventory_error
import com.peakdevcol.project.hotelmanagement.resources.inventory_new_product
import com.peakdevcol.project.hotelmanagement.resources.inventory_no_categories
import com.peakdevcol.project.hotelmanagement.resources.inventory_no_categories_title
import com.peakdevcol.project.hotelmanagement.resources.inventory_select_message
import com.peakdevcol.project.hotelmanagement.resources.inventory_select_title
import com.peakdevcol.project.hotelmanagement.resources.inventory_taxes
import com.peakdevcol.project.hotelmanagement.resources.inventory_taxes_hint
import com.peakdevcol.project.hotelmanagement.resources.menu_inventory
import com.peakdevcol.project.hotelmanagement.resources.retry
import com.peakdevcol.project.hotelmanagement.resources.taxes_inactive
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.AmountText
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDetailLayout
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDetailMinWidth
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIcon
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.SectionLabel
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalShowsMessages
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-INV-01: the products by category, with their price, tax and whether they are active. */
@Composable
fun InventoryScreen(
    onOpenProduct: (productId: String?) -> Unit,
    onCategoriesClick: () -> Unit,
    onTaxesClick: () -> Unit,
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InventoryEffect.NavigateToProduct -> onOpenProduct(effect.productId)
                InventoryEffect.NavigateToCategories -> onCategoriesClick()
                InventoryEffect.NavigateToTaxes -> onTaxesClick()
            }
        }
    }

    InventoryContent(
        state = state,
        onIntent = viewModel::onIntent,
        productForm = { productId, onClose -> ProductFormScreen(productId = productId, onBack = onClose) },
    )
}

/** On wide windows the chosen product opens beside the list, drawn by [productForm]. */
@Composable
fun InventoryContent(
    state: InventoryState,
    onIntent: (InventoryIntent) -> Unit,
    productForm: @Composable (productId: String?, onClose: () -> Unit) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val besideList = maxWidth >= ListDetailMinWidth
        Scaffold(
            topBar = {
                MajesticTopBar(title = stringResource(Res.string.menu_inventory)) {
                    if (besideList && state.canCreateProducts) {
                        MajesticButton(
                            text = stringResource(Res.string.inventory_new_product),
                            onClick = { onIntent(InventoryIntent.NewProduct(besideList = true)) },
                            icon = MajesticIcons.add,
                            modifier = Modifier.padding(end = MajesticSpacing.space3),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!besideList && state.canCreateProducts) {
                    MajesticFloatingActionButton(
                        text = stringResource(Res.string.inventory_new_product),
                        icon = MajesticIcons.add,
                        onClick = { onIntent(InventoryIntent.NewProduct(besideList = false)) },
                    )
                }
            },
            snackbarHost = { AppMessagesHost() },
            containerColor = MajesticTheme.colors.surface,
        ) { padding ->
            if (besideList) {
                ListDetailLayout(
                    modifier = Modifier.padding(padding),
                    list = { InventoryList(state = state, onIntent = onIntent, besideList = true) },
                    detail = { OpenProductPane(openProduct = state.openProduct, onIntent = onIntent, productForm = productForm) },
                )
            } else {
                InventoryList(state = state, onIntent = onIntent, besideList = false, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun OpenProductPane(
    openProduct: OpenProduct?,
    onIntent: (InventoryIntent) -> Unit,
    productForm: @Composable (productId: String?, onClose: () -> Unit) -> Unit,
) {
    if (openProduct == null) {
        EmptyState(
            icon = MajesticIcons.inventory,
            title = stringResource(Res.string.inventory_select_title),
            message = stringResource(Res.string.inventory_select_message),
        )
        return
    }
    CompositionLocalProvider(LocalShowsMessages provides false) {
        key(openProduct) { productForm(openProduct.productId) { onIntent(InventoryIntent.CloseProduct) } }
    }
}

@Composable
private fun InventoryList(state: InventoryState, onIntent: (InventoryIntent) -> Unit, besideList: Boolean, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MajesticSpacing.space4,
            end = MajesticSpacing.space4,
            top = MajesticSpacing.space2,
            // Room for the floating button, so it never covers the last product.
            bottom = if (besideList) MajesticSpacing.space4 else MajesticSizes.floatingActionButtonClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(MajesticSpacing.space3),
    ) {
        item(key = "settings") { InventorySettings(onIntent) }
        when {
            state.isLoading -> item(key = "loading") { MajesticLoading() }
            state.loadFailed -> item(key = "error") {
                EmptyState(
                    icon = MajesticIcons.error,
                    title = stringResource(Res.string.inventory_error),
                    message = stringResource(Res.string.error_check_connection),
                    actionText = stringResource(Res.string.retry),
                    onAction = { onIntent(InventoryIntent.Retry) },
                )
            }
            state.needsCategories -> item(key = "no-categories") {
                EmptyState(
                    icon = MajesticIcons.category,
                    title = stringResource(Res.string.inventory_no_categories_title),
                    message = stringResource(Res.string.inventory_no_categories),
                    actionText = stringResource(Res.string.inventory_create_category),
                    onAction = { onIntent(InventoryIntent.ManageCategories) },
                )
            }
            else -> products(inventory = state.inventory, openProduct = state.openProduct.takeIf { besideList }, onIntent = onIntent, besideList = besideList)
        }
    }
}

/** RF-INV-04 and RF-INV-07: the categories and the taxes, one touch away from the products. */
@Composable
private fun InventorySettings(onIntent: (InventoryIntent) -> Unit) {
    ListGroup {
        SettingsRow(
            title = stringResource(Res.string.inventory_categories),
            subtitle = stringResource(Res.string.inventory_categories_hint),
            icon = MajesticIcons.category,
            onClick = { onIntent(InventoryIntent.ManageCategories) },
        )
        ListDivider()
        SettingsRow(
            title = stringResource(Res.string.inventory_taxes),
            subtitle = stringResource(Res.string.inventory_taxes_hint),
            icon = MajesticIcons.percent,
            onClick = { onIntent(InventoryIntent.ManageTaxes) },
        )
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: DrawableResource, onClick: () -> Unit) {
    ListRow(
        title = title,
        subtitle = subtitle,
        leading = { MajesticIcon(icon = icon, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        trailing = { MajesticIcon(icon = MajesticIcons.chevronRight, contentDescription = null, tint = MajesticTheme.colors.inkMuted) },
        onClick = onClick,
    )
}

private fun LazyListScope.products(inventory: Inventory, openProduct: OpenProduct?, onIntent: (InventoryIntent) -> Unit, besideList: Boolean) {
    inventory.categories.forEach { category ->
        productGroup(
            key = "category-${category.id}",
            title = { category.name },
            products = inventory.productsOf(category),
            inventory = inventory,
            openProduct = openProduct,
            onIntent = onIntent,
            besideList = besideList,
        )
    }
    val withoutCategory = inventory.productsWithoutCategory
    if (withoutCategory.isNotEmpty()) {
        productGroup(
            key = "category-none",
            title = { stringResource(Res.string.category_none) },
            products = withoutCategory,
            inventory = inventory,
            openProduct = openProduct,
            onIntent = onIntent,
            besideList = besideList,
        )
    }
}

private fun LazyListScope.productGroup(
    key: String,
    title: @Composable () -> String,
    products: List<InventoryProduct>,
    inventory: Inventory,
    openProduct: OpenProduct?,
    onIntent: (InventoryIntent) -> Unit,
    besideList: Boolean,
) {
    item(key = "$key-title") { SectionLabel(text = title(), modifier = Modifier.padding(top = MajesticSpacing.space2)) }
    item(key = key) {
        if (products.isEmpty()) {
            Text(text = stringResource(Res.string.inventory_category_empty), style = MajesticTheme.typography.body, color = MajesticTheme.colors.inkMuted)
            return@item
        }
        ListGroup {
            products.forEachIndexed { index, product ->
                if (index > 0) ListDivider()
                ProductRow(
                    product = product,
                    taxLabel = inventory.taxOf(product)?.label(),
                    selected = openProduct?.productId == product.id,
                    onClick = { onIntent(InventoryIntent.SelectProduct(product.id, besideList)) },
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: InventoryProduct, taxLabel: String?, selected: Boolean, onClick: () -> Unit) {
    val inactive = if (product.active) null else stringResource(Res.string.taxes_inactive)
    ListRow(
        title = product.name,
        subtitle = listOfNotNull(inactive, taxLabel).joinToString(" · ").ifEmpty { null },
        leading = if (selected) ({ MajesticIcon(icon = MajesticIcons.check, contentDescription = null, tint = MajesticTheme.colors.primary) }) else null,
        trailing = { AmountText(amount = product.unitPrice, style = MajesticTheme.typography.label.copy(fontSize = MajesticTheme.typography.bodyLarge.fontSize, fontFeatureSettings = "tnum")) },
        onClick = onClick,
    )
}
