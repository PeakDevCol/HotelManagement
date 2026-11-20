package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveDirection
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.categories_delete
import com.peakdevcol.project.hotelmanagement.resources.categories_delete_message
import com.peakdevcol.project.hotelmanagement.resources.categories_empty
import com.peakdevcol.project.hotelmanagement.resources.categories_empty_title
import com.peakdevcol.project.hotelmanagement.resources.categories_move_down
import com.peakdevcol.project.hotelmanagement.resources.categories_move_up
import com.peakdevcol.project.hotelmanagement.resources.categories_name
import com.peakdevcol.project.hotelmanagement.resources.categories_new
import com.peakdevcol.project.hotelmanagement.resources.categories_rename
import com.peakdevcol.project.hotelmanagement.resources.error_check_connection
import com.peakdevcol.project.hotelmanagement.resources.inventory_categories
import com.peakdevcol.project.hotelmanagement.resources.inventory_error
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ButtonStyle
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ConfirmDialog
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.EmptyState
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListDivider
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListGroup
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.ListRow
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticFloatingActionButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticIconButton
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticLoading
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTextField
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.MajesticTopBar
import com.peakdevcol.project.hotelmanagement.ui.designsystem.components.TaskSheet
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticIcons
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSizes
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticSpacing
import com.peakdevcol.project.hotelmanagement.ui.designsystem.theme.MajesticTheme
import com.peakdevcol.project.hotelmanagement.ui.main.AppMessagesHost
import com.peakdevcol.project.hotelmanagement.ui.main.LocalAppMessages
import com.peakdevcol.project.hotelmanagement.ui.main.LocalWideWindow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** RF-INV-04: the categories, in the order the product picker shows them. */
@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CategoriesEffect.Failed -> messages.show(getString(effect.error.message()))
            }
        }
    }

    CategoriesContent(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun CategoriesContent(state: CategoriesState, onIntent: (CategoriesIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = { MajesticTopBar(title = stringResource(Res.string.inventory_categories), onBack = onBack) },
        floatingActionButton = {
            MajesticFloatingActionButton(
                text = stringResource(Res.string.categories_new),
                icon = MajesticIcons.add,
                onClick = { onIntent(CategoriesIntent.AddClick) },
            )
        },
        snackbarHost = { AppMessagesHost() },
        containerColor = MajesticTheme.colors.surface,
    ) { padding ->
        when {
            state.isLoading -> MajesticLoading(Modifier.padding(padding))
            state.loadFailed -> EmptyState(
                icon = MajesticIcons.error,
                title = stringResource(Res.string.inventory_error),
                message = stringResource(Res.string.error_check_connection),
                modifier = Modifier.padding(padding),
            )
            state.categories.isEmpty() -> EmptyState(
                icon = MajesticIcons.category,
                title = stringResource(Res.string.categories_empty_title),
                message = stringResource(Res.string.categories_empty),
                modifier = Modifier.padding(padding),
            )
            else -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MajesticSizes.content)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = MajesticSpacing.space4, end = MajesticSpacing.space4, top = MajesticSpacing.space2, bottom = MajesticSizes.floatingActionButtonClearance),
                ) {
                    ListGroup {
                        state.categories.forEachIndexed { index, category ->
                            if (index > 0) ListDivider()
                            CategoryRow(
                                category = category,
                                isFirst = index == 0,
                                isLast = index == state.categories.lastIndex,
                                enabled = !state.isWorking,
                                onIntent = onIntent,
                            )
                        }
                    }
                }
            }
        }
    }
    CategoryDialogs(state = state, onIntent = onIntent)
}

/** Touching the name renames it; the arrows change its place and the bin deletes it. */
@Composable
private fun CategoryRow(category: Category, isFirst: Boolean, isLast: Boolean, enabled: Boolean, onIntent: (CategoriesIntent) -> Unit) {
    val colors = MajesticTheme.colors
    ListRow(
        title = category.name,
        onClick = if (enabled) ({ onIntent(CategoriesIntent.RenameClick(category)) }) else null,
        trailing = {
            Row {
                MajesticIconButton(
                    icon = MajesticIcons.arrowUp,
                    contentDescription = stringResource(Res.string.categories_move_up, category.name),
                    onClick = { onIntent(CategoriesIntent.Move(category.id, MoveDirection.UP)) },
                    tint = colors.inkMuted,
                    enabled = enabled && !isFirst,
                )
                MajesticIconButton(
                    icon = MajesticIcons.arrowDown,
                    contentDescription = stringResource(Res.string.categories_move_down, category.name),
                    onClick = { onIntent(CategoriesIntent.Move(category.id, MoveDirection.DOWN)) },
                    tint = colors.inkMuted,
                    enabled = enabled && !isLast,
                )
                MajesticIconButton(
                    icon = MajesticIcons.delete,
                    contentDescription = stringResource(Res.string.categories_delete, category.name),
                    onClick = { onIntent(CategoriesIntent.DeleteClick(category)) },
                    tint = colors.danger,
                    enabled = enabled,
                )
            }
        },
    )
}

@Composable
private fun CategoryDialogs(state: CategoriesState, onIntent: (CategoriesIntent) -> Unit) {
    val dismiss = { onIntent(CategoriesIntent.DismissDialog) }
    when (val dialog = state.dialog) {
        is CategoryDialog.Name -> TaskSheet(
            title = dialog.category?.let { stringResource(Res.string.categories_rename, it.name) } ?: stringResource(Res.string.categories_new),
            onDismiss = dismiss,
            asDialog = LocalWideWindow.current,
        ) {
            MajesticTextField(
                value = dialog.text,
                onValueChange = { onIntent(CategoriesIntent.NameChange(it)) },
                label = stringResource(Res.string.categories_name),
                error = dialog.error?.let { stringResource(it.message()) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(CategoriesIntent.ConfirmDialog) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MajesticSpacing.space2, Alignment.End)) {
                MajesticButton(text = stringResource(Res.string.cancel), onClick = dismiss, style = ButtonStyle.Text)
                MajesticButton(
                    text = stringResource(Res.string.save),
                    onClick = { onIntent(CategoriesIntent.ConfirmDialog) },
                    enabled = !state.isWorking,
                    loading = state.isWorking,
                )
            }
        }
        is CategoryDialog.Delete -> ConfirmDialog(
            title = stringResource(Res.string.categories_delete, dialog.category.name),
            message = stringResource(Res.string.categories_delete_message),
            confirmText = stringResource(Res.string.categories_delete, dialog.category.name),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onIntent(CategoriesIntent.ConfirmDialog) },
            onDismiss = dismiss,
            destructive = true,
        )
        null -> Unit
    }
}
