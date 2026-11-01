package com.peakdevcol.project.hotelmanagement.core.dialog

import android.R
import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.peakdevcol.project.hotelmanagement.core.ex.loseFocusAfterAction
import com.peakdevcol.project.hotelmanagement.core.ex.onTextChanged
import com.peakdevcol.project.hotelmanagement.databinding.AddOrderDialogBinding
import com.peakdevcol.project.hotelmanagement.domain.Product


class AddOrderDialog(private val context: Context) {

    private var dialog: AlertDialog? = null

    private lateinit var selectProduct: Product

    private val products = listOf(
        Product("1", "Cerveza Aguila", "0", "3500"),
        Product("2", "Cerveza Poker", "0", "3500"),
        Product("3", "Coca-Cola", "0", "3000"),
        Product("4", "Corona", "0", "6000"),
        Product("5", "Papas BBQ", "0", "2000"),
        Product("6", "Bombombun", "0", "1000"),
        Product("7", "Piscina", "0", "7000"),
        Product("8", "Granizada", "0", "6500"),
        Product("9", "Frappe", "0", "4500")
    )

    private fun setupListeners(
        binding: AddOrderDialogBinding,
        destinationOrder: (Product) -> Unit
    ) {
        setupProductSelection(binding)
        setupQuantityInput(binding)
        setupAcceptButton(binding, destinationOrder)
    }

    private fun setupProductSelection(binding: AddOrderDialogBinding) {
        setUpSearchProduct(binding)
        binding.actvSearchProduct.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
        binding.actvSearchProduct.onTextChanged {
            binding.actvSearchProduct.post {
                binding.actvSearchProduct.setSelection(0)
            }
            enableAddOrderButton(binding)
        }
    }

    private fun setupQuantityInput(binding: AddOrderDialogBinding) {
        binding.etNum.loseFocusAfterAction(EditorInfo.IME_ACTION_DONE)
        binding.etNum.onTextChanged {
            enableAddOrderButton(binding)
        }
    }

    private fun setupAcceptButton(binding: AddOrderDialogBinding, destinationOrder: (Product) -> Unit) {
        binding.acceptBtn.setOnClickListener {
            val quantityProduct = binding.etNum.text.toString()
            destinationOrder.invoke(selectProduct.setQuantityProduct(quantityProduct))
            dismiss() // Close dialog
        }
    }


    private fun setUpSearchProduct(binding: AddOrderDialogBinding) {
        val adapter = ArrayAdapter(
            context,
            R.layout.simple_dropdown_item_1line,
            products.map { it.productName }
        )

        (binding.actvSearchProduct as? MaterialAutoCompleteTextView)?.setAdapter(adapter)

        (binding.actvSearchProduct as? MaterialAutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedProductName = adapter.getItem(position)
            selectProduct = products.firstOrNull { it.productName == selectedProductName }!!

        }
    }

    private fun enableAddOrderButton(binding: AddOrderDialogBinding) {
        val isQuantityEmpty = binding.etNum.text.isNullOrEmpty()
        val isProductSelected = binding.actvSearchProduct.text.isNotEmpty()
        binding.acceptBtn.isEnabled = !isQuantityEmpty && isProductSelected
    }

    fun show(destinationOrder: (Product) -> Unit) {
        if (dialog?.isShowing == true) return

        val binding = AddOrderDialogBinding.inflate(LayoutInflater.from(context))
        setupListeners(binding, destinationOrder)

        dialog = AlertDialog.Builder(context).apply {
            setView(binding.root)
            setCancelable(true)
        }.create().apply {
            setOnDismissListener { dialog = null }  // Limpiar el diálogo al cerrarse
            show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    companion object {
        fun create(context: Context): AddOrderDialog {
            return AddOrderDialog(context)
        }
    }
}
