package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakdevcol.project.hotelmanagement.core.Event
import com.peakdevcol.project.hotelmanagement.domain.Product
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeOrder
import com.peakdevcol.project.hotelmanagement.domain.SaveOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddOrderViewModel @Inject constructor(
    private val saveOrderUseCase: SaveOrderUseCase
) : ViewModel() {

    private val _viewState = MutableStateFlow(AddOrderViewState())
    val viewState: StateFlow<AddOrderViewState> = _viewState

    /** true when the order was saved, false when it failed. */
    private val _orderSaved = MutableLiveData<Event<Boolean>>()
    val orderSaved: LiveData<Event<Boolean>> = _orderSaved

    fun saveProduct(selectedProduct: Product) {
        _viewState.update { it.copy(products = it.products + selectedProduct) }
    }

    fun removeProductList(product: Product) {
        _viewState.update { it.copy(products = it.products - product) }
    }

    fun setTypeOrder(typeOrder: ProviderTypeOrder) {
        _viewState.update { it.copy(typeOrder = typeOrder) }
    }

    fun confirmOrder() {
        val state = _viewState.value
        val typeOrder = state.typeOrder ?: return
        if (!state.confirmButtonEnable) return

        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true) }
            val saved = saveOrderUseCase(state.products, typeOrder)
            _viewState.update {
                if (saved) AddOrderViewState() else it.copy(isLoading = false)
            }
            _orderSaved.value = Event(saved)
        }
    }

}
