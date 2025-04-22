package com.fiap.cutwatch.view.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiap.cutwatch.domain.model.User
import com.fiap.cutwatch.domain.state.RequestState
import com.fiap.cutwatch.domain.usecase.LoginUseCase
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val logionUseCase: LoginUseCase) : ViewModel() {

    private val _loginState = MutableStateFlow<RequestState<FirebaseUser>?>(null)
    val loginState : StateFlow<RequestState<FirebaseUser>?> = _loginState

    fun login(user: User) {
        viewModelScope.launch {
            if (user.email.isNullOrBlank().not() || user.password.isNullOrBlank().not()) {
                _loginState.value = RequestState.Loading
                val response = logionUseCase.login(user)
                _loginState.value = response
            } else {
                _loginState.value = RequestState.Error(Exception("Favor preencher todos os campos"))
            }
        }
    }
}