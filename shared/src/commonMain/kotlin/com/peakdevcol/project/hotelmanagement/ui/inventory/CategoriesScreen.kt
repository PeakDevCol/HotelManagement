package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveDirection
import com.peakdevcol.project.hotelmanagement.resources.Res
import com.peakdevcol.project.hotelmanagement.resources.cancel
import com.peakdevcol.project.hotelmanagement.resources.categories_delete
import com.peakdevcol.project.hotelmanagement.resources.categories_delete_message
import com.peakdevcol.project.hotelmanagement.resources.categories_empty
import com.peakdevcol.project.hotelmanagement.resources.categories_move_down
import com.peakdevcol.project.hotelmanagement.resources.categories_move_up
import com.peakdevcol.project.hotelmanagement.resources.categories_name
import com.peakdevcol.project.hotelmanagement.resources.categories_new
import com.peakdevcol.project.hotelmanagement.resources.categories_rename
import com.peakdevcol.project.hotelmanagement.resources.ic_add
import com.peakdevcol.project.hotelmanagement.resources.ic_arrow_back
import com.peakdevcol.project.hotelmanagement.resources.ic_delete
import com.peakdevcol.project.hotelmanagement.resources.ic_edit
import com.peakdevcol.project.hotelmanagement.resources.inventory_categories
import com.peakdevcol.project.hotelmanagement.resources.inventory_error
import com.peakdevcol.project.hotelmanagement.resources.save
import com.peakdevcol.project.hotelmanagement.ui.components.ScreenTopBar
import com.peakdevcol.project.hotelmanagement.ui.currentorders.CenteredMessage
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CategoriesEffect.Failed -> snackbarHostState.showSnackbar(getString(effect.error.message()))
            }
        }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(Res.string.inventory_categories), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onIntent(CategoriesIntent.AddClick) }) {
                Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.categories_new))
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed -> CenteredMessage(stringResource(Res.string.inventory_error), Modifier.padding(padding))
            state.categories.isEmpty() -> CenteredMessage(stringResource(Res.string.categories_empty), Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                itemsIndexed(state.categories, key = { _, category -> category.id }) { index, category ->
                    CategoryRow(
                        category = category,
                        isFirst = index == 0,
                        isLast = index == state.categories.lastIndex,
                        enabled = !state.isWorking,
                        onIntent = viewModel::onIntent,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    when (val dialog = state.dialog) {
        is CategoryDialog.Name -> CategoryNameDialog(dialog, enabled = !state.isWorking, onIntent = viewModel::onIntent)
        is CategoryDialog.Delete -> AlertDialog(
            onDismissRequest = { viewModel.onIntent(CategoriesIntent.DismissDialog) },
            title = { Text(stringResource(Res.string.categories_delete, dialog.category.name)) },
            text = { Text(stringResource(Res.string.categories_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(CategoriesIntent.ConfirmDialog) }) {
                    Text(stringResource(Res.string.categories_delete, dialog.category.name))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(CategoriesIntent.DismissDialog) }) { Text(stringResource(Res.string.cancel)) }
            },
        )
        null -> Unit
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isFirst: Boolean,
    isLast: Boolean,
    enabled: Boolean,
    onIntent: (CategoriesIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // The back arrow, turned, stands for "up" and "down": the project has no other arrow drawing.
        IconButton(onClick = { onIntent(CategoriesIntent.Move(category.id, MoveDirection.UP)) }, enabled = enabled && !isFirst) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = stringResource(Res.string.categories_move_up, category.name),
                modifier = Modifier.rotate(90f),
            )
        }
        IconButton(onClick = { onIntent(CategoriesIntent.Move(category.id, MoveDirection.DOWN)) }, enabled = enabled && !isLast) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = stringResource(Res.string.categories_move_down, category.name),
                modifier = Modifier.rotate(-90f),
            )
        }
        IconButton(onClick = { onIntent(CategoriesIntent.RenameClick(category)) }, enabled = enabled) {
            Icon(
                painter = painterResource(Res.drawable.ic_edit),
                contentDescription = stringResource(Res.string.categories_rename, category.name),
            )
        }
        IconButton(onClick = { onIntent(CategoriesIntent.DeleteClick(category)) }, enabled = enabled) {
            Icon(
                painter = painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.categories_delete, category.name),
            )
        }
    }
}

@Composable
private fun CategoryNameDialog(dialog: CategoryDialog.Name, enabled: Boolean, onIntent: (CategoriesIntent) -> Unit) {
    val title = dialog.category?.let { stringResource(Res.string.categories_rename, it.name) } ?: stringResource(Res.string.categories_new)
    AlertDialog(
        onDismissRequest = { onIntent(CategoriesIntent.DismissDialog) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = dialog.text,
                onValueChange = { onIntent(CategoriesIntent.NameChange(it)) },
                label = { Text(stringResource(Res.string.categories_name)) },
                isError = dialog.error != null,
                supportingText = dialog.error?.let { error -> { Text(stringResource(error.message())) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onIntent(CategoriesIntent.ConfirmDialog) }, enabled = enabled) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(CategoriesIntent.DismissDialog) }) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
