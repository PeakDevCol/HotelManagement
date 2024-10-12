package com.peakdevcol.project.hotelmanagement.ui.addorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.peakdevcol.project.hotelmanagement.core.dialog.AddOrderDialog
import com.peakdevcol.project.hotelmanagement.core.dialog.SelectTypeOrderDialog
import com.peakdevcol.project.hotelmanagement.databinding.FragmentAddOrderBinding
import com.peakdevcol.project.hotelmanagement.domain.Product
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeOrder
import com.peakdevcol.project.hotelmanagement.ui.addorder.adapter.AddOrderAdapter
import com.peakdevcol.project.hotelmanagement.ui.home.HomeViewModel
import com.peakdevcol.project.hotelmanagement.utils.HotelManagementConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddOrderFragment : Fragment() {


    private var _binding: FragmentAddOrderBinding? = null
    private val binding get() = _binding!!

    private val addOrderViewModel: AddOrderViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var addOrderAdapter: AddOrderAdapter

    private var addOrderDialog: AddOrderDialog? = null

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

    private fun initUi() {
        initRecyclerView()
        initListeners()
        initObservers()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                addOrderViewModel.viewState.collect { viewState ->
                    updateUi(viewState)
                }
            }
        }

        addOrderViewModel.currentProductList.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Añade productos", Toast.LENGTH_SHORT).show()
                addOrderAdapter.updateList(it)
            } else {
                addOrderAdapter.updateList(it)
            }
        }
    }

    private fun updateUi(viewState: AddOrderViewState) {
        when (viewState) {
            is AddOrderViewState.ButtonEnable -> {
                binding.confirmBtn.isEnabled = viewState.confirmButtonEnable
            }

            AddOrderViewState.Error -> {}
            AddOrderViewState.Loading -> {}
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
                dialog.show() {

                    //REVISAR COMO AÑADIR LAS LISTAS DEL PROYECTO DE LAS DONAS
                    /*       addOrderAdapter.updateList(
                               listOf(
                                   Product(
                                       productId = "1",
                                       productName = product.substringBefore(","),
                                       productQuantity = product.substringAfter(","),
                                       payAmount =

                                   )
                               )
                           )*/
                    dialog.dismiss()
                }
            }

            confirmBtn.setOnClickListener {

            }
        }
    }

    private fun getTypeOrder(s: String): ProviderTypeOrder {
        val type = s.substringBefore(":")
        val num = s.substringAfter(":").trim().toInt()
        return when (type) {
            HotelManagementConstants.ROOM -> ProviderTypeOrder.Room(num)
            HotelManagementConstants.TABLE -> ProviderTypeOrder.Table(num)
            else -> ProviderTypeOrder.Other
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