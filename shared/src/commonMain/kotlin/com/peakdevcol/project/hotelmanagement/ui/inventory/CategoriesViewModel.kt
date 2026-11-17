package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.usecase.DeleteCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveCategoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.MoveDirection
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveCategoryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the administrator is doing to one category. */
sealed interface CategoryDialog {
    /** Creating a category, or renaming [category] when it is not null. */
    data class Name(val category: Category?, val text: String, val error: InventoryError? = null) : CategoryDialog
    data class Delete(val category: Category) : CategoryDialog
}

data class CategoriesState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val categories: List<Category> = emptyList(),
    val dialog: CategoryDialog? = null,
    val isWorking: Boolean = false,
)

sealed interface CategoriesIntent {
    data object AddClick : CategoriesIntent
    data class RenameClick(val category: Category) : CategoriesIntent
    data class DeleteClick(val category: Category) : CategoriesIntent
    data class NameChange(val text: String) : CategoriesIntent
    data object ConfirmDialog : CategoriesIntent
    data object DismissDialog : CategoriesIntent
    data class Move(val categoryId: String, val direction: MoveDirection) : CategoriesIntent
}

sealed interface CategoriesEffect {
    data class Failed(val error: InventoryError) : CategoriesEffect
}

class CategoriesViewModel(
    observeInventory: ObserveInventoryUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val moveCategory: MoveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    private val _effects = Channel<CategoriesEffect>(Channel.BUFFERED)
    val effects: Flow<CategoriesEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeInventory()
                .catch { _state.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { inventory -> _state.update { it.copy(isLoading = false, categories = inventory.categories) } }
        }
    }

    fun onIntent(intent: CategoriesIntent) {
        when (intent) {
            CategoriesIntent.AddClick -> _state.update { it.copy(dialog = CategoryDialog.Name(category = null, text = "")) }
            is CategoriesIntent.RenameClick ->
                _state.update { it.copy(dialog = CategoryDialog.Name(intent.category, intent.category.name)) }
            is CategoriesIntent.DeleteClick -> _state.update { it.copy(dialog = CategoryDialog.Delete(intent.category)) }
            is CategoriesIntent.NameChange -> changeName(intent.text)
            CategoriesIntent.ConfirmDialog -> confirmDialog()
            CategoriesIntent.DismissDialog -> _state.update { it.copy(dialog = null) }
            is CategoriesIntent.Move -> work { moveCategory(intent.categoryId, intent.direction) }
        }
    }

    private fun changeName(text: String) = _state.update { state ->
        val dialog = state.dialog as? CategoryDialog.Name ?: return@update state
        state.copy(dialog = dialog.copy(text = text.take(MAX_NAME_LENGTH), error = null))
    }

    private fun confirmDialog() {
        when (val dialog = _state.value.dialog) {
            is CategoryDialog.Name -> saveName(dialog)
            is CategoryDialog.Delete -> {
                _state.update { it.copy(dialog = null) }
                work { deleteCategory(dialog.category.id) }
            }
            null -> Unit
        }
    }

    // A wrong name keeps the dialog open with its error; anything else closes it.
    private fun saveName(dialog: CategoryDialog.Name) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val error = saveCategory(dialog.category?.id, dialog.text).exceptionOrNull().toInventoryError()
            val nameError = error.takeIf { it in NAME_ERRORS }
            _state.update { it.copy(isWorking = false, dialog = nameError?.let { dialog.copy(error = nameError) }) }
            if (error != null && nameError == null) _effects.send(CategoriesEffect.Failed(error))
        }
    }

    private fun work(change: suspend () -> Result<Unit>) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val error = change().exceptionOrNull().toInventoryError()
            _state.update { it.copy(isWorking = false) }
            if (error != null) _effects.send(CategoriesEffect.Failed(error))
        }
    }

    private fun Throwable?.toInventoryError(): InventoryError? =
        this?.let { it as? InventoryError ?: InventoryError.Unknown }

    private companion object {
        const val MAX_NAME_LENGTH = 30
        val NAME_ERRORS = setOf(InventoryError.BlankName, InventoryError.DuplicateCategory)
    }
}
