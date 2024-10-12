package com.peakdevcol.project.hotelmanagement.ui.addorder

sealed class AddOrderViewState() {
    data object Loading : AddOrderViewState()
    data object Error : AddOrderViewState()
    data class ButtonEnable(val addButtonEnable: Boolean = false, val confirmButtonEnable: Boolean = false) : AddOrderViewState()

}
