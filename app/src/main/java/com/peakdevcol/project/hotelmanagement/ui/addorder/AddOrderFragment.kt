package com.peakdevcol.project.hotelmanagement.ui.addorder

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.core.dialog.AddOrderDialog
import com.peakdevcol.project.hotelmanagement.core.dialog.BasicDialog
import com.peakdevcol.project.hotelmanagement.core.dialog.LoadingDialog
import com.peakdevcol.project.hotelmanagement.core.dialog.SelectTypeOrderDialog
import com.peakdevcol.project.hotelmanagement.databinding.FragmentAddOrderBinding
import com.peakdevcol.project.hotelmanagement.domain.Product
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeOrder
import com.peakdevcol.project.hotelmanagement.ui.addorder.adapter.AddOrderAdapter
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class AddOrderFragment : Fragment() {


    private var _binding: FragmentAddOrderBinding? = null
    private val binding get() = _binding!!

    private val addOrderViewModel: AddOrderViewModel by viewModels()

    private lateinit var addOrderAdapter: AddOrderAdapter

    private var addOrderDialog: AddOrderDialog? = null

    private val amountFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LoadingDialog.dismiss()
        _binding = null
    }

    private fun initUi() {
        initRecyclerView()
        initListeners()
        initObservers()
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                addOrderViewModel.viewState.collect { viewState ->
                    updateUi(viewState)
                }
            }
        }

        addOrderViewModel.orderSaved.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { saved ->
                showOrderResult(saved)
            }
        }
    }

    private fun updateUi(viewState: AddOrderViewState) {
        with(binding) {
            addOrderAdapter.updateList(viewState.products)
            txtTypeOrder.text = viewState.typeOrder?.toDisplayText()
                ?: getString(R.string.order_destination_default)
            txtTotal.text = getString(R.string.order_total, amountFormat.format(viewState.totalAmount))
            confirmBtn.isEnabled = viewState.confirmButtonEnable
        }

        if (viewState.isLoading) {
            LoadingDialog.create(requireContext())
        } else {
            LoadingDialog.dismiss()
        }
    }

    private fun initListeners() {
        with(binding) {
            addActionButton.setOnClickListener {
                if (addOrderDialog == null) {
                    addOrderDialog = AddOrderDialog.create(requireContext())
                }
                addOrderDialog?.show { selectedProduct ->
                    addOrderViewModel.saveProduct(selectedProduct)
                }
            }

            icEditOrder.setOnClickListener {
                val dialog = SelectTypeOrderDialog.create(requireContext())
                dialog.show { typeOrder ->
                    addOrderViewModel.setTypeOrder(typeOrder)
                    dialog.dismiss()
                }
            }

            confirmBtn.setOnClickListener {
                addOrderViewModel.confirmOrder()
            }
        }
    }

    private fun ProviderTypeOrder.toDisplayText(): String = when (this) {
        is ProviderTypeOrder.Room -> "${HotelManagementConstants.ROOM} $roomNumber"
        is ProviderTypeOrder.Table -> "${HotelManagementConstants.TABLE} $tableNumber"
        ProviderTypeOrder.Other -> HotelManagementConstants.OTHER
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showOrderResult(saved: Boolean) {
        val title = if (saved) R.string.title_order_saved else R.string.title_order_error
        val msg = if (saved) R.string.supporting_text_order_saved else R.string.supporting_text_order_error
        BasicDialog.create(
            requireContext(),
            resources.getDrawable(R.drawable.dialog_bg, requireActivity().theme),
            getString(title),
            getString(msg),
            getString(R.string.accept_order)
        ) {
            it.dismiss()
        }
    }

    private fun initRecyclerView() {
        setUpLayoutManager()
        setUpAdapter(listOf())
    }

    private fun setUpLayoutManager() {
        val layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean = true
        }
        binding.recyclerView.layoutManager = layoutManager
    }

    private fun setUpAdapter(orders: List<Product>) {
        addOrderAdapter = AddOrderAdapter(orders) {
            addOrderViewModel.removeProductList(it)
        }
        binding.recyclerView.adapter = addOrderAdapter
    }

}
