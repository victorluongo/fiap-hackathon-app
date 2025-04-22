package com.fiap.cutwatch.view.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fiap.cutwatch.R
import com.fiap.cutwatch.databinding.FragmentFormLoginBinding
import com.fiap.cutwatch.domain.extensions.onBackPress
import com.fiap.cutwatch.domain.state.RequestState

import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * A simple [Fragment] subclass.
 * Use the [FormLoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FormLoginFragment : Fragment() {

    private val binding: FragmentFormLoginBinding by lazy {
        FragmentFormLoginBinding.inflate(layoutInflater)
    }

    private val viewModel: FormViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
        initObservers()
        onBackPress()
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createUserState.collect {
                when (it) {
                    is RequestState.Success -> {
                        binding.ivLogin.visibility = View.INVISIBLE
                        findNavController().navigate(R.id.action_formLoginFragment_to_loginFragment)
                    }
                    is RequestState.Error -> {
                        binding.layoutLoading.loadingAnimationLogin.visibility = View.INVISIBLE
                        binding.tvPasswordFeedback.text = getString(R.string.fail_create_user)
                    }
                    is RequestState.Loading -> {
                        binding.layoutLoading.loadingAnimationLogin.visibility = View.VISIBLE
                    }

                    else -> {}
                }

            }
        }
    }

    private fun initButtons() {
        binding.buttonCreatedAccount.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etValidPassword.text.toString()

            val isValid = viewModel.validUser(name, email, password, confirmPassword)

            if (isValid) {
                viewModel.createUser(email, password)

            } else {
                binding.tvPasswordFeedback.text = getString(R.string.fail_create_user)
            }
        }
    }
}