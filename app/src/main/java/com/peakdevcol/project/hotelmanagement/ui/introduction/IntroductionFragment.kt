package com.peakdevcol.project.hotelmanagement.ui.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.databinding.FragmentIntroductionBinding

class IntroductionFragment : Fragment() {

    private var _binding: FragmentIntroductionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IntroductionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroductionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        initObservers()
        initListeners()
    }

    private fun initObservers() {
    }

    private fun initListeners() {
        binding.loginBtn.setOnClickListener {
            findNavController().navigate(R.id.action_introductionFragment_to_loginFragment)
        }

        binding.signInBtn.setOnClickListener {
            findNavController().navigate(R.id.action_introductionFragment_to_signInFragment)
        }
    }
}