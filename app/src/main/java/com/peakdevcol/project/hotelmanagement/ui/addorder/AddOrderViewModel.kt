package com.peakdevcol.project.hotelmanagement.ui.addorder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peakdevcol.project.hotelmanagement.domain.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class AddOrderViewModel @Inject constructor() : ViewModel() {

    private val _viewState = MutableStateFlow<AddOrderViewState>(AddOrderViewState.Loading)
    val viewState: StateFlow<AddOrderViewState> = _viewState

    private var _currentProductList: MutableLiveData<List<Product>> = MutableLiveData()
    val currentProductList: LiveData<List<Product>> = _currentProductList

    fun setAddButtonEnable(enable: Boolean) {
        _viewState.value = AddOrderViewState.ButtonEnable(addButtonEnable = enable)
    }

    fun setConfirmButtonEnable(enable: Boolean) {
        _viewState.value = AddOrderViewState.ButtonEnable(confirmButtonEnable = enable)
    }

    fun setProductList(newOrder: Product?, preOrder: List<Product>?) {
        var currentList = preOrder ?: listOf()
        if (newOrder != null)
            currentList = currentList.plus(newOrder)
        _currentProductList.value = currentList
    }

    fun saveProduct(selectedProduct: Product) {
        setCurrentProducList(selectedProduct, currentProductList.value ?: listOf())
    }

    private fun setCurrentProducList(selectedProduct: Product?, preProductsOrder: List<Product>?) {
        var currentList = preProductsOrder ?: listOf()
        if (selectedProduct != null)
            currentList = currentList.plus(selectedProduct)
        _currentProductList.value = currentList

    }


}