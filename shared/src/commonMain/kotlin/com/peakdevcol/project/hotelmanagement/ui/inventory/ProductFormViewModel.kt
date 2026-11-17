package com.peakdevcol.project.hotelmanagement.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.domain.model.Category
import com.peakdevcol.project.hotelmanagement.domain.model.InventoryError
import com.peakdevcol.project.hotelmanagement.domain.usecase.ObserveInventoryUseCase
import com.peakdevcol.project.hotelmanagement.domain.usecase.SaveProductUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductFormState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val notFound: Boolean = false,
    val name: String = "",
    /** Pesos without decimals, as the administrator types them. */
    val priceText: String = "",
    val categoryId: String? = null,
    val active: Boolean = true,
    val categories: List<Category> = emptyList(),
    /** The field the last attempt to save found wrong. */
    val fieldError: InventoryError? = null,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !notFound && !isSaving
}

sealed interface ProductFormIntent {
    data class NameChange(val name: String) : ProductFormIntent
    data class PriceChange(val priceText: String) : ProductFormIntent
    data class CategorySelect(val categoryId: String) : ProductFormIntent
    data class ActiveChange(val active: Boolean) : ProductFormIntent
    data object Save : ProductFormIntent
}

sealed interface ProductFormEffect {
    data object Saved : ProductFormEffect
    data class SaveFailed(val error: InventoryError) : ProductFormEffect
}

class ProductFormViewModel(
    private val productId: String?,
    private val observeInventory: ObserveInventoryUseCase,
    private val saveProduct: SaveProductUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFormState(isNew = productId == null))
    val state: StateFlow<ProductFormState> = _state.asStateFlow()

    private val _effects = Channel<ProductFormEffect>(Channel.BUFFERED)
    val effects: Flow<ProductFormEffect> = _effects.receiveAsFlow()

    // The product is not carried over from the edited one, so a tax set on it is kept when saving.
    private var taxId: String? = null

    init {
        loadForm()
    }

    fun onIntent(intent: ProductFormIntent) {
        when (intent) {
            is ProductFormIntent.NameChange -> edit { it.copy(name = intent.name.take(MAX_NAME_LENGTH)) }
            is ProductFormIntent.PriceChange ->
                edit { it.copy(priceText = intent.priceText.filter(Char::isDigit).take(MAX_PRICE_DIGITS)) }
            is ProductFormIntent.CategorySelect -> edit { it.copy(categoryId = intent.categoryId) }
            is ProductFormIntent.ActiveChange -> edit { it.copy(active = intent.active) }
            ProductFormIntent.Save -> save()
        }
    }

    // The form is filled once: later changes from the server must not overwrite what is being typed.
    private fun loadForm() {
        viewModelScope.launch {
            val inventory = runCatching { observeInventory().first() }.getOrElse {
                _state.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            val product = inventory.products.firstOrNull { it.id == productId }
            taxId = product?.taxId
            _state.update { state ->
                when {
                    productId == null -> state.copy(isLoading = false, categories = inventory.categories)
                    product == null -> state.copy(isLoading = false, notFound = true)
                    else -> state.copy(
                        isLoading = false,
                        name = product.name,
                        priceText = product.unitPrice.toString(),
                        categoryId = product.categoryId.takeIf { id -> inventory.categories.any { it.id == id } },
                        active = product.active,
                        categories = inventory.categories,
                    )
                }
            }
        }
    }

    private fun edit(change: (ProductFormState) -> ProductFormState) =
        _state.update { change(it).copy(fieldError = null) }

    private fun save() {
        val form = _state.value
        if (!form.canSave) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveProduct(
                productId = productId,
                name = form.name,
                unitPrice = form.priceText.toLongOrNull(),
                categoryId = form.categoryId,
                taxId = taxId,
                active = form.active,
            )
            val error = result.exceptionOrNull()?.let { it as? InventoryError ?: InventoryError.Unknown }
            _state.update { it.copy(isSaving = false, fieldError = error.takeIf { it in FIELD_ERRORS }) }
            when (error) {
                null -> _effects.send(ProductFormEffect.Saved)
                in FIELD_ERRORS -> Unit
                else -> _effects.send(ProductFormEffect.SaveFailed(error))
            }
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
        const val MAX_PRICE_DIGITS = 9
        val FIELD_ERRORS = setOf(InventoryError.BlankName, InventoryError.InvalidPrice, InventoryError.MissingCategory)
    }
}
