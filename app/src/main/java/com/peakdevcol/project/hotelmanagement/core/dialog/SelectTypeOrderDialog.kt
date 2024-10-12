package com.peakdevcol.project.hotelmanagement.core.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.peakdevcol.project.hotelmanagement.core.ex.onTextChanged
import com.peakdevcol.project.hotelmanagement.databinding.SelectTypeOrderDialogBinding
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeOrder
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants


class SelectTypeOrderDialog(private val context: Context) {

    private var dialog: AlertDialog? = null
    private var radioSelected = false
    private lateinit var typeOrder : ProviderTypeOrder

    private fun acceptButtonEnabled(binding: SelectTypeOrderDialogBinding) {
        val number = binding.tilTypeOrder.text.toString()
        binding.acceptBtn.isEnabled = number.isNotEmpty()
    }

    private fun setupListeners(
        binding: SelectTypeOrderDialogBinding,
        destinationOrder: (String) -> Unit
    ) {
        with(binding) {
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    radioButtonHab.id -> {
                        configureEditText(binding, HotelManagementConstants.ROOM)
                    }

                    radioButtonTable.id -> {
                        configureEditText(binding, HotelManagementConstants.TABLE)
                    }

                    radioButtonOther.id -> {
                        etTypeOrder.hint = HotelManagementConstants.OTHER
                        etTypeOrder.isEnabled = false
                        tilTypeOrder.text?.clear()
                        acceptBtn.isEnabled = true
                    }
                }
            }

            tilTypeOrder.onTextChanged {
                if (radioSelected) {
                    acceptButtonEnabled(binding)
                }
            }

            acceptBtn.setOnClickListener {
                destinationOrder.invoke("${etTypeOrder.hint}:  ${tilTypeOrder.text}")
            }
        }

    }

    private fun configureEditText(binding: SelectTypeOrderDialogBinding, hint: String) {
        binding.etTypeOrder.hint = hint
        binding.etTypeOrder.isEnabled = true
        binding.tilTypeOrder.text?.clear()
        binding.acceptBtn.isEnabled = false
        acceptButtonEnabled(binding)
        radioSelected = true
    }

    fun show(destinationOrder: (String) -> Unit) {
        if (dialog?.isShowing == true) return

        val binding = SelectTypeOrderDialogBinding.inflate(LayoutInflater.from(context))
        setupListeners(binding, destinationOrder)

        dialog = AlertDialog.Builder(context).apply {
            setView(binding.root)
            setCancelable(true)
        }.create()

        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    companion object {
        fun create(context: Context): SelectTypeOrderDialog {
            return SelectTypeOrderDialog(context)
        }
    }
}
