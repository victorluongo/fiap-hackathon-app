package com.fiap.cutwatch.view.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fiap.cutwatch.R

import com.fiap.cutwatch.domain.extensions.hideKeyboard
import com.fiap.cutwatch.domain.extensions.onBackPress
import com.fiap.cutwatch.domain.model.User
import com.fiap.cutwatch.domain.state.RequestState
import com.fiap.cutwatch.view.viewmodel.EstadoAppViewModel
import com.fiap.cutwatch.databinding.FragmentLoginBinding

import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

   // private val REQUEST_CODE_GOOGLE_SIGN_IN = 1 /* unique request id */
   // lateinit var gso: GoogleSignInOptions
   // lateinit var gsc: GoogleSignInClient
    private val viewModel: LoginViewModel by viewModel()
    private val estadoAppViewModel: EstadoAppViewModel by sharedViewModel()
    private val binding: FragmentLoginBinding by lazy {
        FragmentLoginBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboard()
        initButtons()
        initObserver()
        onBackPress()
    }

    private fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect {
                when (it) {
                    is RequestState.Success -> {
                        binding.loadingAnimationLogin.visibility = View.INVISIBLE

                        findNavController().navigate(R.id.action_loginFragment_to_listaKniveFragment)
                    }

                    is RequestState.Error -> {
                        val animShake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                        binding.containerLogin.startAnimation(animShake)
                        binding.tvPasswordFeedback.text = it.throwable.message
                        binding.loadingAnimationLogin.visibility = View.INVISIBLE
                    }

                    is RequestState.Loading -> {
                        binding.loadingAnimationLogin.visibility = View.VISIBLE
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }
        estadoAppViewModel.limpaComponentes()
    }

        private fun initButtons() {
            binding.singUpButton.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_formLoginFragment)
            }

            binding.buttonLogin.setOnClickListener {
                val email = binding.editTextEmailAddressLogin.text.toString()
                val password = binding.editTextPasswordLogin.text.toString()

                lifecycleScope.launch {
                    viewModel.login(User(email, password))
                }
            }
        }

       /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            /* if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        findNavController().navigate(R.id.action_formularioImagem_to_listaImagens)
                    }
                } catch (e: Exception) {
                    e.stackTrace
                } */
            }
        }*/
    }
