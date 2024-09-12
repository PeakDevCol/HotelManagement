package com.peakdevcol.project.hotelmanagement.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.core.NavigationEvent
import com.peakdevcol.project.hotelmanagement.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeFragmentViewModel: HomeFragmentViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel.setTopAppBarTitle(getString(R.string.top_bar_menu))
        initUi()
    }

    private fun initUi() {
        initListeners()
    }

    private fun initListeners() {
        binding.balanceBtn.setOnClickListener {
            homeViewModel.navigate(HomeViewState.Navigate(NavigationEvent.NavigationToBalance))
            homeViewModel.setTopAppBarTitle(getString(R.string.top_bar_balance))
        }
        binding.inventoryBtn.setOnClickListener {
            homeViewModel.navigate(HomeViewState.Navigate(NavigationEvent.NavigationToInventory))
            homeViewModel.setTopAppBarTitle(getString(R.string.top_bar_inventory))

        }
        binding.addOrderBtn.setOnClickListener {
            homeViewModel.navigate(HomeViewState.Navigate(NavigationEvent.NavigationToAddOrders))
            homeViewModel.setTopAppBarTitle(getString(R.string.top_bar_add_order))

        }
        binding.currentOrderBtn.setOnClickListener {
            homeViewModel.navigate(HomeViewState.Navigate(NavigationEvent.NavigationToCurrentOrders))
            homeViewModel.setTopAppBarTitle(getString(R.string.top_bar_current_order))
        }
    }

}