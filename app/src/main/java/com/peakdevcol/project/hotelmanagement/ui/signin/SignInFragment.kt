package com.peakdevcol.project.hotelmanagement.ui.signin

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
import com.peakdevcol.project.hotelmanagement.databinding.FragmentSignInBinding
import com.peakdevcol.project.hotelmanagement.domain.ProviderLogin
import com.peakdevcol.project.hotelmanagement.domain.ProviderTypeUser
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionViewModel
import com.peakdevcol.project.hotelmanagement.ui.introduction.IntroductionViewState
import com.peakdevcol.project.hotelmanagement.ui.signin.model.FullUserSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private val signInViewModel: SignInViewModel by viewModels()
    private val introductionViewModel: IntroductionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
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

    private fun initListeners() {
        with(binding) {
            etFullName.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
            etFullName.onTextChanged { onFieldChanged() }

            etEmail.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
            etEmail.onTextChanged { onFieldChanged() }

            etPassword.loseFocusAfterAction(EditorInfo.IME_ACTION_NEXT)
            etPassword.onTextChanged { onFieldChanged() }

            etConfirmPassword.loseFocusAfterAction(EditorInfo.IME_ACTION_DONE)
            etConfirmPassword.onTextChanged { onFieldChanged() }

            btnCreateAccount.setOnClickListener {
                it.dismissKeyboard()
                signInViewModel.onSignInSelected(
                    FullUserSignIn(
                        fullName = etFullName.text.toString(),
                        email = etEmail.text.toString(),
                        password = etPassword.text.toString(),
                        passwordConfirmation = etConfirmPassword.text.toString(),
                        provider = ProviderLogin.BASIC,
                        type = ProviderTypeUser.BASIC
                    ), {
                        introductionViewModel.setViewState(infoError())
                    }, { loading ->
                        introductionViewModel.setViewState(loading)
                    }
                )
            }
        }
    }

    private fun onFieldChanged(hasFocus: Boolean = false) {
        if (!hasFocus) {
            signInViewModel.onFieldsChanged(
                FullUserSignIn(
                    fullName = binding.etFullName.text.toString(),
                    email = binding.etEmail.text.toString(),
                    password = binding.etPassword.text.toString(),
                    passwordConfirmation = binding.etConfirmPassword.text.toString(),
                    provider = ProviderLogin.BASIC,
                    type = ProviderTypeUser.BASIC
                )
            )
        }
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                signInViewModel.viewState.collect { viewState ->
                    updateUi(viewState)
                }
            }
        }

        signInViewModel.navigateToHome.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                goToHome()
            }
        }

    }

    private fun updateUi(viewState: SignInViewState) {
        with(binding) {
            tilFullName.error =
                if (viewState.isValidFullName) null else getString(R.string.full_name_error)
            tilEmail.error = if (viewState.isValidEmail) null else getString(R.string.email_error)
            tilPassword.error =
                if (viewState.isValidPassword) null else getString(R.string.password_error)
            tilConfirmPassword.error =
                if (viewState.isValidPassword) null else getString(R.string.confirm_password_error)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun infoError(): IntroductionViewState.Error {
        return IntroductionViewState.Error(
            requireContext(),
            resources.getDrawable(R.drawable.dialog_bg, requireActivity().theme),
            resources.getString(R.string.title_sign_in),
            resources.getString(R.string.supporting_text_sign_in),
            resources.getString(R.string.accept_sign_in)
        )

    }

    private fun goToHome() {
        //startActivity(HomeActivity.create(requireActivity()))
    }
}