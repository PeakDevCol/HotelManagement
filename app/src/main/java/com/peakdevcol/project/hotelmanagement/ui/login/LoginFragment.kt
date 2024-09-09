package com.peakdevcol.project.hotelmanagement.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.peakdevcol.project.hotelmanagement.R
import com.peakdevcol.project.hotelmanagement.core.ex.dismissKeyboard
import com.peakdevcol.project.hotelmanagement.core.ex.loseFocusAfterAction
import com.peakdevcol.project.hotelmanagement.core.ex.onTextChanged
import com.peakdevcol.project.hotelmanagement.databinding.FragmentLoginBinding
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionViewModel
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {


    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginFragmentViewModel by viewModels()
    private val introductionViewModel: IntroductionViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

    /**
     * I chose the scopeFunction .with instead of .apply because when you use .apply you
     * need to change properties of the object and return it. But when you only need to work
     * with the object context and you don´t need to return it. .with is the best choice.
     */
    private fun initListeners() {
        with(binding) {
            etEmail.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
            etEmail.onTextChanged { onFieldChanged() }

            binding.etPassword.loseFocusAfterAction(EditorInfo.IME_ACTION_DONE)
            etPassword.onTextChanged { onFieldChanged() }

            btnLogin.setOnClickListener {
                it.dismissKeyboard()
                loginViewModel.onLoginSelected(etEmail.text.toString(), etPassword.text.toString(),
                    {
                        introductionViewModel.setViewState(infoError())
                    }, { loading ->
                        introductionViewModel.setViewState(loading)
                    })
            }
        }

    }

    private fun onFieldChanged() {
        loginViewModel.onFieldsChanged(
            email = binding.etEmail.text.toString(),
            password = binding.etPassword.text.toString()
        )
    }

    private fun initObservers() {
        // use viewLifecycleOwner because in this form the coroutine are match with the
        // lifecycle of fragment, but if you only use lifecycle the coroutine its match for activity lifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.viewState.collect { viewState ->
                    updateUi(viewState)
                }
            }
        }

        loginViewModel.navigateToHome.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                goToHome()
            }

        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun infoError(): IntroductionViewState.Error {
        return IntroductionViewState.Error(
            requireContext(),
            resources.getDrawable(R.drawable.dialog_bg, requireActivity().theme),
            resources.getString(R.string.title_login),
            resources.getString(R.string.supporting_text_login),
            resources.getString(R.string.accept_login)
        )

    }

    private fun updateUi(viewState: LoginViewState) {
        with(binding) {
            tilEmail.error =
                if (viewState.isValidEmail) null else requireActivity().getString(R.string.wrong_email)
            tilPassword.error =
                if (viewState.isValidPassword) null else requireActivity().getString(R.string.wrong_password)
        }
    }

    private fun goToHome() {
        //startActivity(HomeActivity.create(requireActivity()))
        introductionViewModel.setViewState(null)
    }

}